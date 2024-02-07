package com.emretaskesen.tpost.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class Notify(context : Context) {

    fun sentFinishNotify(
        context:Context,
        title: String?,
        message : String?,
        icon: Int,
        intent : Intent?
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context,
            ConstVal.Notifications.CHANNEL_ID)
            .setAutoCancel(true)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setSound(defaultSoundUri)
        if (intent!=null){
            notificationBuilder.setContentIntent(pendingIntent)
        }


        val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}