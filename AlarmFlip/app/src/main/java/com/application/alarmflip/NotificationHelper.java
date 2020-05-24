package com.application.alarmflip;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper extends ContextWrapper {
    public static final String channelID = "channelID";
    public static final String channelName = "Channel Name";

    private NotificationManager mManager;

    public NotificationHelper(Context base)
    {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            createChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel()
    {
        NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }

    public NotificationManager getManager()
    {
        if (mManager == null)
        {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    Intent activityIntent = new Intent(this, MainActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

    public NotificationCompat.Builder getChannelNotification()
    {
        return new NotificationCompat.Builder(getApplicationContext(), channelID)
                .setContentTitle("Alarm Flip")
                .setContentText("Tap here to open Alarm Flip app.")
                .setSmallIcon(R.drawable.ic_alarm)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
    }
}
