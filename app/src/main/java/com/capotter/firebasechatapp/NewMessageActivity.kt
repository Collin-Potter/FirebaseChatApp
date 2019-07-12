package com.capotter.firebasechatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row_new_direct_message.view.*

class NewMessageActivity : AppCompatActivity() {

    public lateinit var db: FirebaseFirestore
    public lateinit var selectedUsersMap: HashMap<Int, UserModel>
    public lateinit var selectedUsers: ArrayList<UserModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        val messageTypeIntent = intent.getStringExtra("type")

        when(messageTypeIntent) {

            "directmessage" -> {

                supportActionBar?.title = "Select User"

                val adapter = GroupAdapter<ViewHolder>()

                recyclerview_newmessage.adapter = adapter

                recyclerview_newmessage.layoutManager = LinearLayoutManager(this)

                db = FirebaseFirestore.getInstance()

                fetchUsersForDirectMessage()

            }

            "groupmessage" -> {

                selectedUsersMap = hashMapOf()
                selectedUsers = ArrayList()

                supportActionBar?.title = "Select Users"

                val adapter = GroupAdapter<ViewHolder>()

                recyclerview_newmessage.adapter = adapter

                recyclerview_newmessage.layoutManager = LinearLayoutManager(this)

                db = FirebaseFirestore.getInstance()

                fetchUsersForGroupMessage()

                //TODO: Create floating action button to create group chat activity once user has selected desired users for group

                val fab: View = findViewById(R.id.createGroupButton)
                fab.visibility = View.VISIBLE
                fab.setOnClickListener { view ->
                    startGroup()
                }

            }
        }
    }

    private fun fetchUsersForDirectMessage() {
        db.collection("users")
            .addSnapshotListener { snapshots, e ->

                val adapter = GroupAdapter<ViewHolder>()
                if (e != null) {
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val user: UserModel = UserModel(
                            userId = dc.document.data["userId"].toString(),
                            displayName = dc.document.data["displayName"].toString(),
                            email = dc.document.data["email"].toString(),
                            userImageUrl = dc.document.data["userImage"].toString()
                        )
                        if(user.displayName!= FirebaseAuth.getInstance().currentUser!!.displayName) {
                            adapter.add(DirectUserItem(user))
                        }
                    }
                }
                adapter.setOnItemClickListener { item, view ->

                    val userItem = item as DirectUserItem
                    val intent= Intent(view.context, ChatLogActivity::class.java)
                    intent.putExtra(USER_KEY, userItem.user)
                    startActivity(intent)
                    finish()

                }

                recyclerview_newmessage.adapter = adapter

            }

    }

    private fun fetchUsersForGroupMessage() {
        db.collection("users")
            .addSnapshotListener { snapshots, e ->

                val adapter = GroupAdapter<ViewHolder>()
                if (e != null) {
                    return@addSnapshotListener
                }
                var iterator = 0
                for (dc in snapshots!!.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val user: UserModel = UserModel(
                            userId = dc.document.data["userId"].toString(),
                            displayName = dc.document.data["displayName"].toString(),
                            email = dc.document.data["email"].toString(),
                            userImageUrl = dc.document.data["userImage"].toString()
                        )
                        if(user.displayName!= FirebaseAuth.getInstance().currentUser!!.displayName) {
                            adapter.add(GroupUserItem(user))
                            selectedUsersMap[iterator] = user
                            iterator++
                        }
                    }

                }
                adapter.setOnItemClickListener { item, view ->
                    val currentItem = adapter.getItem(adapter.getAdapterPosition(item)) as GroupUserItem
                    if(currentItem.getActive()) {
                        currentItem.setActive(false)
                        view.setBackgroundColor(resources.getColor(R.color.colorToPrimary))
                        selectedUsersMap[adapter.getAdapterPosition(item)]!!.active = false
                    }else{
                        currentItem.setActive(true)
                        view.setBackgroundColor(resources.getColor(R.color.colorSelected))
                        selectedUsersMap[adapter.getAdapterPosition(item)]!!.active = true
                    }
                }
                recyclerview_newmessage.adapter = adapter

            }

    }

    private fun startGroup() {
        //TODO: Check all users in list to see which is selected and add them to selectedUsers before moving on

        for(i in 0 until selectedUsersMap.size) { // for users in map
            when(selectedUsersMap[i]?.active) { // if user is active
//                selectedUsers.add() // add user to selectedUsers
                true -> {
                    selectedUsers.add(selectedUsersMap[i]!!)
                    Log.d(TAG, selectedUsersMap[i]!!.userId + " added to group...")
                }
                else -> {
                    Log.d(TAG, "User: " + selectedUsersMap[i]!!.userId + " | Active Status: " + selectedUsersMap[i]!!.active)
                }
            }
        }

        if(selectedUsers.size > 1) {

            //TODO: Check for previous existence of unique group key before creating duplicate
            val deviceGroup = object {
                var operation = "create"
                var notification_key_name = ""
                var registration_ids = mutableListOf<String>()
            }

            //TODO: Create unique group key
            deviceGroup.notification_key_name = "PLACEHOLDER"

            for(i in 0 until selectedUsers.size){

                deviceGroup.registration_ids.add(selectedUsers[i].registrationId)

            }

            val createGroupIntent = Intent(this@NewMessageActivity, ChatLogActivity::class.java)


            createGroupIntent.putParcelableArrayListExtra("members", selectedUsers)
            startActivity(createGroupIntent)
        }else{
            Toast.makeText(this@NewMessageActivity, "Please select at least two users", Toast.LENGTH_SHORT).show()
        }

    }

    companion object {
        const val TAG = "NewMessageActivity"
        val USER_KEY = "USER_KEY"
    }
}

class DirectUserItem(val user: UserModel): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userDisplayName.text = user.displayName
        Picasso.with(viewHolder.itemView.context)
            .load(user.userImageUrl)
            .transform(CircleTransform())
            .resize(100,100)
            .centerCrop()
            .into(viewHolder.itemView.userProfileImage)
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_direct_message
    }
}

class GroupUserItem(val user: UserModel): Item<ViewHolder>() {
    var userActivated = false
    fun getActive(): Boolean{
        return userActivated
    }
    fun setActive(bool: Boolean){
        userActivated = bool
    }
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.userDisplayName.text = user.displayName
        Picasso.with(viewHolder.itemView.context)
            .load(user.userImageUrl)
            .transform(CircleTransform())
            .resize(100,100)
            .centerCrop()
            .into(viewHolder.itemView.userProfileImage)
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_group_message
    }
}

