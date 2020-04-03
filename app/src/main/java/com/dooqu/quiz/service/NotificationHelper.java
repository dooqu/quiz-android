package com.dooqu.quiz.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.lang.ref.WeakReference;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationHelper<T extends Service> {
    WeakReference<T> conversationServiceWeakReference;
    private Notification servieNotification;
    private NotificationCompat.Builder notifyBuilder;
    private int notifyId = android.os.Process.myPid();
    NotificationManager notificationManager;

    public NotificationHelper(T conversationService, int iconResId) {
        conversationServiceWeakReference = new WeakReference<>(conversationService);
        createNotification(iconResId);
    }

    private Notification createNotification(int iconResId) {
        notificationManager = (NotificationManager) conversationServiceWeakReference.get().getSystemService(NOTIFICATION_SERVICE);
        notifyBuilder = new NotificationCompat.Builder(conversationServiceWeakReference.get());
        notifyBuilder.setContentTitle("开心词典语音版");
        notifyBuilder.setContentText("竞赛中");
        notifyBuilder.setSmallIcon(iconResId);
        notifyBuilder.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE);
        notifyBuilder.setAutoCancel(false);
        notifyBuilder.setShowWhen(false);
        notifyBuilder.setOngoing(true);
        notifyBuilder.setSound(null);
        notifyBuilder.setVibrate(new long[]{0});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "cn.dooqu.quiz";
            String channelName = "开心词典语音版";
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannel.setSound(null, null);

            notificationManager.createNotificationChannel(notificationChannel);
            //设定builder的channelid
            notifyBuilder.setChannelId(channelId);
        }
        servieNotification = notifyBuilder.build();
        return servieNotification;
    }

    public void retainForeground() {
        conversationServiceWeakReference.get().startForeground(notifyId, servieNotification);
    }

    public void cancelForeground() {
        conversationServiceWeakReference.get().stopForeground(true);
    }

    public void updateContent(String content) {
        notifyBuilder.setContentText(content);
        notificationManager.notify(notifyId, notifyBuilder.build());
    }

    public void reset() {
        updateContent("就绪中");
    }
}
