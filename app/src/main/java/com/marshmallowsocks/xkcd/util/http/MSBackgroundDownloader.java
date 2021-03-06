package com.marshmallowsocks.xkcd.util.http;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.activities.msxkcd;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;
import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MSBackgroundDownloader extends Service {

    private Context context;
    private MSXkcdDatabase db;
    private int maxDownloadSize;
    private Fetch fetchQueue;
    private AtomicInteger completeDownloads;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    public MSBackgroundDownloader() {

    }

    public void startDownloads() {
        final int appId = 72345;
        completeDownloads = new AtomicInteger(0);
        List<Request> downloadRequests = new ArrayList<>();
        mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle("Downloading comics")
                .setContentText("0/1836")
                .setSmallIcon(R.drawable.ms_stat_msxkcd)
                .setAutoCancel(true)
                .setOngoing(true)
                .setProgress(100, 0, false)
                .setContentIntent(PendingIntent.getActivity(context, appId, new Intent(context, msxkcd.class), PendingIntent.FLAG_CANCEL_CURRENT));
        mNotifyManager.notify(appId, mBuilder.build());
        List<XKCDComicBean> allComics = db.getAllComics();
        maxDownloadSize = allComics.size();

        int index;
        for(index = 0; index < maxDownloadSize; index++) {
            XKCDComicBean comic = allComics.get(index);
            if(isInteractive(comic)) {
                completeDownloads.incrementAndGet();
                continue;
            }
            File comicFile = new File(getFilesDir().getAbsolutePath() + "/xkcd_" + comic.getNumber() + comic.getImageUrl().substring(comic.getImageUrl().length() - 4));
            if(!comicFile.exists()) {
                downloadRequests.add(new Request(comic.getImageUrl(), getFilesDir().getAbsolutePath(), "xkcd_" + comic.getNumber() + comic.getImageUrl().substring(comic.getImageUrl().length() - 4)));
            }
            else {
                completeDownloads.incrementAndGet();
            }
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
                                .setProgress(0, 0, false)
                                .setContentIntent(PendingIntent.getActivity(context, appId, new Intent(context, msxkcd.class), PendingIntent.FLAG_CANCEL_CURRENT));
                        mNotifyManager.notify(appId, mBuilder.build());
                        MSBackgroundDownloader.this.onDestroy();

                    }
                }
                if(status == Fetch.STATUS_ERROR) {
                    completeDownloads.incrementAndGet();
                }
            }
        });
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.SYNC_IN_PROGRESS, true);
        editor.apply();
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
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constants.OFFLINE_COUNT, completeDownloads.intValue());
        editor.remove(Constants.SYNC_IN_PROGRESS);
        editor.apply();
        editor.apply();
        fetchQueue.release();
        db.close();
        stopSelf();
    }
}