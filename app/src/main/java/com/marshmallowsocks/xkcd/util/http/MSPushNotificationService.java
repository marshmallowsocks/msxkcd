package com.marshmallowsocks.xkcd.util.http;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.marshmallowsocks.xkcd.util.core.Constants;

/**
 * Created by marshmallowsocks on 5/6/2017.
 * Handles new comic notifications.
 */

public class MSPushNotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        Intent newComicIntent = new Intent();
        newComicIntent.setAction(Constants.NEW_COMIC_ADDED);
        newComicIntent.putExtra(Constants.NEW_COMIC_ADDED, remoteMessage.getNotification().getBody());
        LocalBroadcastManager.getInstance(this).sendBroadcast(newComicIntent);
    }
}
