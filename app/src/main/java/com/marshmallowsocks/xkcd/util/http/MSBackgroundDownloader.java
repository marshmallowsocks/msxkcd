package com.marshmallowsocks.xkcd.util.http;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;
import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MSBackgroundDownloader extends Service {

    private Context context;
    private MSXkcdDatabase db;
    private int maxDownloadSize;
    private int index;
    private Fetch fetchQueue;
    private List<XKCDComicBean> allComics;
    private List<Request> downloadRequests;
    private AtomicInteger completeDownloads;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    public MSBackgroundDownloader() {

    }

    public void startDownloads() {
        final int appId = 72345;
        completeDownloads = new AtomicInteger(0);
        downloadRequests = new ArrayList<>();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle("Downloading comics")
                .setContentText("0/1836")
                .setSmallIcon(R.drawable.ms_stat_msxkcd)
                .setAutoCancel(true)
                .setOngoing(true)
                .setProgress(100, 0, false);
        mNotifyManager.notify(appId, mBuilder.build());
        allComics = db.getAllComics();
        maxDownloadSize = allComics.size();

        for(index = 0; index < maxDownloadSize; index++) {
            XKCDComicBean comic = allComics.get(index);
            if(isInteractive(comic)) {
                completeDownloads.incrementAndGet();
                continue;
            }
            downloadRequests.add(new Request(comic.getImageUrl(), getFilesDir().getAbsolutePath(), "xkcd_" + comic.getNumber() + comic.getImageUrl().substring(comic.getImageUrl().length() - 4)));
        }
        fetchQueue.addFetchListener(new FetchListener() {
            @Override
            public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {
                if(status == Fetch.STATUS_DONE) {
                    completeDownloads.incrementAndGet();
                    int p = (int) (completeDownloads.intValue() / ((float) maxDownloadSize) * 100);
                    mBuilder.setContentText(completeDownloads + "/" + maxDownloadSize).setProgress(100, p, false);
                    mNotifyManager.notify(appId, mBuilder.build());
                    Log.d("DOWNLOADER", completeDownloads.toString());

                    if (completeDownloads.intValue() == maxDownloadSize) {
                        Log.d("DOWNLOADER", "complete");
                        mBuilder.setContentText("Download complete")
                                .setProgress(0, 0, false);
                        mNotifyManager.notify(appId, mBuilder.build());
                        mNotifyManager.cancel(appId);
                    }
                }
                if(status == Fetch.STATUS_ERROR) {
                    completeDownloads.incrementAndGet();
                }
            }
        });
        fetchQueue.enqueue(downloadRequests);
    }

    private boolean isInteractive(XKCDComicBean comic) {
        return comic != null && !(comic.getImageUrl().endsWith(".png") || comic.getImageUrl().endsWith(".jpg") || comic.getImageUrl().endsWith(".gif"));
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        db = new MSXkcdDatabase(context);
        fetchQueue = Fetch.getInstance(context);
        new Fetch.Settings(getApplicationContext())
                .setAllowedNetwork(Fetch.NETWORK_ALL)
                .enableLogging(true)
                .setConcurrentDownloadsLimit(3)
                .apply();
        this.context = context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                startDownloads();
            }
        }).start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        db.close();
    }
}