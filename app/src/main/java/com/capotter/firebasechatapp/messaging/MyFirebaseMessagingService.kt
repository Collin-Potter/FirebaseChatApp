package com.capotter.firebasechatapp.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.capotter.firebasechatapp.LoginRegisterActivity
import com.capotter.firebasechatapp.R
import com.capotter.firebasechatapp.UsersListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "From: ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
//            Log.d(TAG, "Message data payload: " + remoteMessage.data)
//            sendNotification(remoteMessage?.notification?.body!!)
            sendNotification(remoteMessage!!.data["body"]!!)
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
//            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(remoteMessage?.notification?.body!!)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
//        sendNotification(remoteMessage?.notification?.body!!)
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        updateUser(FirebaseAuth.getInstance().currentUser!!, token)
    }

    private fun updateUser(user: FirebaseUser, token: String?) {
        val userDetails = hashMapOf(
            "userId" to user.uid,
            "displayName" to user.displayName.toString(),
            "email" to user.email.toString(),
            "userImage" to user.photoUrl.toString(),
            "registrationId" to token
        )
        FirebaseFirestore.getInstance().collection("users").document(userDetails["userId"].toString())
            .set(userDetails)
            .addOnSuccessListener { /*Log.i(TAG, "DocumentSnapshot successfully written!")*/ }
            .addOnFailureListener { /*exception -> Log.w(TAG, "Error writing document", exception)*/ }
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, UsersListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("New Message")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
    companion object {
        const val TAG = "MyFirebaseMessagingService"
    }

}
