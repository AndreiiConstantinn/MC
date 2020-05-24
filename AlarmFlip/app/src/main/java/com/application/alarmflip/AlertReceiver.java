package com.application.alarmflip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import androidx.core.app.NotificationCompat;

public class AlertReceiver extends BroadcastReceiver
{

    public static MediaPlayer alarmSound;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        NotificationHelper notificationHelper = new NotificationHelper(context);
        NotificationCompat.Builder nb = notificationHelper.getChannelNotification();
        notificationHelper.getManager().notify(1, nb.build());

        MainActivity.startAlarmFlag = true;
        alarmSound.start();
    }
}
