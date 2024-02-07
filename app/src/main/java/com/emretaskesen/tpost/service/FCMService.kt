package com.emretaskesen.tpost.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.emretaskesen.tpost.ui.activity.user.NotificationPage
import com.emretaskesen.tpost.util.ConstVal.Notifications.CHANNEL_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okio.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class FCMService : FirebaseMessagingService() {
    private val myName = FirebaseAuth.getInstance().currentUser?.displayName
    val db = Firebase.firestore
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type: String? = remoteMessage.data["messageType"]
        val title: String? = remoteMessage.data["title"]
        val body: String? = remoteMessage.data["body"]
        val senderProfile: String? = remoteMessage.data["profileImage"]
        val userIDs: String? = remoteMessage.data["userIDs"]
        val message: String? = remoteMessage.data["message"]
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        when(sharedPreferences.getBoolean(getString(com.emretaskesen.tpost.R.string.key_app_notification), true)){
           true ->{
               when (type){
                   "message" ->{
                       sentMessageNotify(myName,body,senderProfile,message,userIDs)
                   }
                   else ->{
                       sendNotification(myName,body,senderProfile,title,type)
                   }
               }
           }
            false->{
                return
            }
        }

    }

    @SuppressLint("LogNotTimber")
    override fun onNewToken(token: String) {
        Log.d("FCM", "The device token has been refreshed: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val userID = FirebaseAuth.getInstance().currentUser!!.uid
        val tokenMap = hashMapOf<String, Any>()
        tokenMap["fcmToken"] = token
        db.collection("Users").document(userID).update(tokenMap)
    }

    private fun sentMessageNotify(
        title : String? ,
        body : String? ,
        senderProfile : String? ,
        message : String? ,
        userID : String? ,
    ) {

        // Key for the string that's delivered in the action's intent.


        val intent = Intent(this, ChatPage::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("userID",userID)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val groupName = "Chat"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val senderProfileImage = getBitmapFromURL(senderProfile)
        val notificationBuilder = NotificationCompat.Builder(this,CHANNEL_ID)
            .setAutoCancel(true)
            .setSmallIcon(com.emretaskesen.tpost.R.drawable.ic_message)
            .setContentTitle(body)
            .setContentText(message)
            .setSound(defaultSoundUri)
            .setSubText(title)
            .setSettingsText(message)
            .setGroup(groupName)
            .setContentIntent(pendingIntent)
            .setLargeIcon(senderProfileImage)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder)
    }

    private fun sendNotification(
        title : String? ,
        body : String? ,
        senderProfile : String? ,
        message : String? ,
        notifyType : String? ,
    ) {
        val intent = Intent(this, NotificationPage::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val senderProfileImage = getBitmapFromURL(senderProfile)
        val notificationBuilder = NotificationCompat.Builder(this,CHANNEL_ID)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(body)
            .setContentText(message)
            .setSound(defaultSoundUri)
            .setSubText(title)
            .setSettingsText(message)
            .setLargeIcon(senderProfileImage)
        when (notifyType){
            "cmmt"->{
                notificationBuilder.setSmallIcon(com.emretaskesen.tpost.R.drawable.ic_notif_comment)
            }
            "like"->{
                notificationBuilder.setSmallIcon(com.emretaskesen.tpost.R.drawable.ic_notif_like)
            }
            "flw"->{
                notificationBuilder.setSmallIcon(com.emretaskesen.tpost.R.drawable.ic_notif_follow)
            }
            "tag"->{
                notificationBuilder.setSmallIcon(com.emretaskesen.tpost.R.drawable.ic_notif_ment)
            }
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getBitmapFromURL(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}


