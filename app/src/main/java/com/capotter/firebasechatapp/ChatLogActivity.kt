package com.capotter.firebasechatapp

import com.capotter.firebasechatapp.helpers.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.capotter.firebasechatapp.messaging.MyFirebaseMessagingService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.lang.Exception

class ChatLogActivity : AppCompatActivity() {


    public lateinit var db: FirebaseFirestore
    public lateinit var user: UserModel
    public lateinit var group: ArrayList<UserModel>
    public lateinit var groupId: String

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        try {
            user = intent.getParcelableExtra(NewMessageActivity.USER_KEY)
            supportActionBar?.title = user.displayName

            db = FirebaseFirestore.getInstance()

            listenForMessages()

            recyclerview_chat_log.adapter = adapter
            recyclerview_chat_log.layoutManager = LinearLayoutManager(this)

            send_button_chat_log.setOnClickListener{
                sendMessage()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                group = intent.getParcelableArrayListExtra("members")
                if(group.size > 3) {
                    supportActionBar?.title = group.get(0).displayName + ", " + group.get(1).displayName + ", " + group.get(2).displayName + " and " + (group.size - 3) + " other(s)."
                }else if(group.size == 3){
                    supportActionBar?.title = group.get(0).displayName + ", " + group.get(1).displayName + " and " + group.get(2).displayName
                }else if(group.size == 2){
                    supportActionBar?.title = group.get(0).displayName + ", " + group.get(1).displayName
                }

                db = FirebaseFirestore.getInstance()

                listenForGroupMessages()

                recyclerview_chat_log.adapter = adapter
                recyclerview_chat_log.layoutManager = LinearLayoutManager(this)

                send_button_chat_log.setOnClickListener{
                    sendGroupMessage()
                }
            } catch (ge: Exception) {
                ge.printStackTrace()
            }
        }

    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid.toString()
        val toId = user.userId
        val docRef = db.collection("user-messages").document(fromId).collection(toId).orderBy("timestamp")
        docRef.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            if(dc.document.data.get("fromId") == FirebaseAuth.getInstance().currentUser!!.uid &&
                                dc.document.data.get("toId") == user.userId){

                                adapter.add(ChatToItem(dc.document.data.get("message").toString(), FirebaseAuth.getInstance().currentUser!!.photoUrl.toString()))
                                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                            }else if(dc.document.data.get("fromId") == user.userId &&
                                dc.document.data.get("toId") == FirebaseAuth.getInstance().currentUser!!.uid){

                                adapter.add(ChatFromItem(dc.document.data.get("message").toString(), user.userImageUrl))
                                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                            }
                        }
                    }
                }

        }

    }

    private fun listenForGroupMessages() {
        groupId = "PLACEHOLDER" //TODO: Subscribe to groupId
    }

    class ChatMessage(val id: String, val message: String, val fromId: String, val toId: String, val timestamp: Long) {
        constructor() : this("","","","",-1)
    }

    class ChatGroupMessage(val id: String, val message: String, val fromId: String, val groupId: String, val timestamp: Long) {
        constructor() : this("","","","",-1)
    }

    private fun sendMessage() {
        if(!edittext_chat_log.text.toString().isNullOrEmpty()) {
            val text = edittext_chat_log.text.toString()
            edittext_chat_log.text.clear()
            val newMessageRef = db.collection("user-messages").document(FirebaseAuth.getInstance().currentUser!!.uid)
                .collection(user.userId).document()
            val newToMessageRef = db.collection("user-messages").document(user.userId)
                .collection(FirebaseAuth.getInstance().currentUser!!.uid).document()
            val message = ChatMessage(
                newMessageRef.id,
                text,
                FirebaseAuth.getInstance().currentUser!!.uid,
                user.userId,
                System.currentTimeMillis() / 1000
            )
            newMessageRef.set(message)
                .addOnSuccessListener {
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
                .addOnFailureListener {  }
            newToMessageRef.set(message)

            val dbOld = FirebaseDatabase.getInstance()

            var fromId = FirebaseAuth.getInstance().currentUser!!.uid
            var toId = user.userId
            var dbOldRef = dbOld.getReference("recent-messages/$fromId/$toId/most-recent")
            dbOldRef.setValue(message)

            fromId = user.userId
            toId = FirebaseAuth.getInstance().currentUser!!.uid
            dbOldRef = dbOld.getReference("recent-messages/$fromId/$toId/most-recent")
            dbOldRef.setValue(message)

            sendNotificationToUser(fromId, toId, message.message)

            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
//                        Log.w(TAG, "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }
                })
            val topic = fromId + "_" + toId
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "Subscribed to " + topic
                    if (!task.isSuccessful) {
                        msg = "Subscription Failed"
                    }
                    Log.d(TAG, msg)
                }

        }
    }

    private fun sendGroupMessage() {
        if(!edittext_chat_log.text.toString().isNullOrEmpty()) {
            val text = edittext_chat_log.text.toString()
            edittext_chat_log.text.clear()
            val newMessageRef = db.collection("user-messages").document(FirebaseAuth.getInstance().currentUser!!.uid)
                .collection(groupId/* TODO: GROUP ID */).document()
            val newToMessageRef = db.collection("user-messages").document(groupId/* TODO: GROUP ID */)
                .collection(FirebaseAuth.getInstance().currentUser!!.uid).document()
            val message = ChatGroupMessage(
                newMessageRef.id,
                text,
                FirebaseAuth.getInstance().currentUser!!.uid,
                groupId/* TODO: GROUP ID */,
                System.currentTimeMillis() / 1000
            )
            newMessageRef.set(message)
                .addOnSuccessListener {
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                }
                .addOnFailureListener {  }
            newToMessageRef.set(message)

            val dbOld = FirebaseDatabase.getInstance()

            var fromId = FirebaseAuth.getInstance().currentUser!!.uid
            var toId = user.userId
            var dbOldRef = dbOld.getReference("recent-messages/$fromId/$toId/most-recent")
            dbOldRef.setValue(message)

            fromId = user.userId
            toId = FirebaseAuth.getInstance().currentUser!!.uid
            dbOldRef = dbOld.getReference("recent-messages/$fromId/$toId/most-recent")
            dbOldRef.setValue(message)

            sendNotificationToUser(fromId, toId, message.message)

            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
//                        Log.w(TAG, "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }
                })
            val topic = fromId + "_" + toId
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "Subscribed to " + topic
                    if (!task.isSuccessful) {
                        msg = "Subscription Failed"
                    }
                    Log.d(TAG, msg)
                }

        }
    }

    companion object {
        val TAG = "ChatLogActivity"
    }
}

class ChatFromItem(val text: String, val userImageUrl: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.chat_from_text.text = text
        Picasso.with(viewHolder.itemView.chat_from_image.context)
            .load(userImageUrl)
            .transform(CircleTransform())
            .resize(50,50)
            .centerCrop()
            .into(viewHolder.itemView.chat_from_image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem(val text: String, val userImageUrl: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.chat_to_text.text = text
        Picasso.with(viewHolder.itemView.chat_to_image.context)
            .load(userImageUrl)
            .transform(CircleTransform())
            .resize(50,50)
            .centerCrop()
            .into(viewHolder.itemView.chat_to_image)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}