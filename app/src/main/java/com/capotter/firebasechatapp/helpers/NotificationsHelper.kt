package com.capotter.firebasechatapp.helpers

import com.google.firebase.firestore.FirebaseFirestore

fun sendNotificationToUser(recipient: String, user: String, message: String) {
    val ref = FirebaseFirestore.getInstance()

    val notifications = ref.collection("notificationRequests")

    val notification = hashMapOf(
        "toUser" to recipient,
        "fromUser" to user,
        "message" to message
    )

    notifications.add(notification)
}