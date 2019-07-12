package com.capotter.firebasechatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_users_list.*
import kotlinx.android.synthetic.main.user_row_recent.view.*
import java.text.SimpleDateFormat
import java.util.*

class UsersListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_list)
        title = "Recent Messages"

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()

        listenForMostRecent()

        userList.adapter = adapter
        userList.layoutManager = LinearLayoutManager(this)
        userList.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener { item, _ ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as RecentMessageRow
            intent.putExtra(USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }

    }

    val recentMessagesMap = HashMap<String, ChatLogActivity.ChatMessage>()

    private fun refreshRecyclerView() {
        adapter.clear()
        val result = recentMessagesMap.toList().sortedByDescending { (_,value) -> value.timestamp }.toMap()
        result.values.forEach {
            adapter.add(RecentMessageRow(it))
        }

    }

    private fun listenForMostRecent() {
        val dbOld = FirebaseDatabase.getInstance()

        var fromId = FirebaseAuth.getInstance().currentUser!!.uid
        var dbOldRef = dbOld.getReference("recent-messages/$fromId")
        dbOldRef.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                p0.children.forEach {
                    val message = it.getValue(ChatLogActivity.ChatMessage::class.java)?: return
                    recentMessagesMap[p0.key.toString()] = message
                    refreshRecyclerView()
                }

            }
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                p0.children.forEach {
                    val message = it.getValue(ChatLogActivity.ChatMessage::class.java)?: return
                    recentMessagesMap[p0.key.toString()] = message
                    refreshRecyclerView()
                }
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onChildRemoved(p0: DataSnapshot) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.newmessage -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                intent.putExtra("type", "directmessage")
                startActivity(intent)
            }
            R.id.signout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginRegisterActivity::class.java)
                startActivity(intent)
            }
            R.id.newgroup -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                intent.putExtra("type", "groupmessage")
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val USER_KEY = "USER_KEY"
        private const val TAG = "UsersListActivity"
    }
}

class RecentMessageRow(val chatMessage: ChatLogActivity.ChatMessage): Item<ViewHolder>() {
    var chatPartnerUser: UserModel? = null
    val db = FirebaseFirestore.getInstance()
    override fun bind(viewHolder: ViewHolder, position: Int) {

        val chatPartnerId: String
        if (chatMessage.fromId == FirebaseAuth.getInstance().currentUser!!.uid){
            chatPartnerId = chatMessage.toId
        }else{
            chatPartnerId = chatMessage.fromId
        }


        var userDisplayName: String
        var userProfileUrl: String

        // Create a reference to the cities collection
        val docRef = db.collection("users")
        docRef.get()
            .addOnCompleteListener {task ->
                if(task.isSuccessful){
                    for(document in task.result!!){
                        if(document["userId"].toString() == chatPartnerId) {
                            val user = UserModel(
                                userId = document.data["userId"].toString(),
                                displayName = document.data["displayName"].toString(),
                                email = document.data["email"].toString(),
                                userImageUrl = document.data["userImage"].toString()
                            )
                            chatPartnerUser = user
                            userDisplayName = document["displayName"].toString()
                            userProfileUrl = document["userImage"].toString()

                            viewHolder.itemView.userProfileName.text = userDisplayName
                            Picasso.with(viewHolder.itemView.userProfileImage.context)
                                .load(userProfileUrl)
                                .transform(CircleTransform())
                                .resize(50,50)
                                .centerCrop()
                                .into(viewHolder.itemView.userProfileImage)
                            if(chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                                if(("Me: " + chatMessage.message).length > 25) {
                                    viewHolder.itemView.recentMessageText.text = ("Me: " + chatMessage.message).substring(0,25) + "..."
                                }else{
                                    viewHolder.itemView.recentMessageText.text = "Me: " + chatMessage.message
                                }
                            } else {
                                if(chatMessage.message.length > 25) {
                                    viewHolder.itemView.recentMessageText.text = chatMessage.message.substring(0,25) + "..."
                                }else{
                                    viewHolder.itemView.recentMessageText.text = chatMessage.message
                                }
                            }
                            fun convertLongToTime(time: Long): String {
                                val date = Date(time)
                                val format = SimpleDateFormat("MM/dd/yyyy HH:mm")
                                return format.format(date)
                            }
                            viewHolder.itemView.dateText.text = convertLongToTime(chatMessage.timestamp*1000)
                        }
                    }
                }else{}
            }
    }

    override fun getLayout(): Int {
        return R.layout.user_row_recent
    }

    companion object {
        private val USER_KEY = "USER_KEY"
        private const val TAG = "UsersListActivity"
    }
}


