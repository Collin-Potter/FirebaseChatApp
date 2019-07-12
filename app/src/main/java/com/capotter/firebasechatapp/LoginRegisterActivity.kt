package com.capotter.firebasechatapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso

class LoginRegisterActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var signInButton: Button
    private lateinit var signOutButton: Button
    private lateinit var continueButton: Button
    private lateinit var emailText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var confirmationText: TextView
    private lateinit var orTextView: TextView
    private lateinit var imageView: ImageView

    // Start auth declaration
    private lateinit var auth: FirebaseAuth
    // End auth declaration

    private lateinit var db: FirebaseFirestore

    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        signInButton = findViewById(R.id.signInButton)
        signOutButton = findViewById(R.id.signOutButton)
        emailText = findViewById(R.id.emailText)
        welcomeText = findViewById(R.id.headerText)
        confirmationText = findViewById(R.id.textView)
        continueButton = findViewById(R.id.continueButton)
        orTextView = findViewById(R.id.textView2)
        imageView = findViewById(R.id.imageView)

        signInButton.setOnClickListener(this)
        signOutButton.setOnClickListener(this)
        continueButton.setOnClickListener(this)

        // Configuring Google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.id_token))
            .requestEmail()
            .build()
        // End configuration

        googleSignInClient = GoogleSignIn.getClient(this,gso)

        // Start auth initialization
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // End auth initialization

        db = FirebaseFirestore.getInstance()

    }


//    private fun sendNotification() {
//        val newMessageNotification = Notification.Builder(this@LoginRegisterActivity, CHANNEL_ID)
//            .setSmallIcon(R.drawable.app_logo)
//            .setContentTitle("You have mail")
//            .setContentText("Some random person has messaged you")
//
//        val name = "Channel Name"
//        val descriptionText = "Channel Description"
//        val importance = NotificationManager.IMPORTANCE_DEFAULT
//        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//            description = descriptionText
//        }
//        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(channel)
//
//        val intent = Intent(this, UsersListActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
//
//        newMessageNotification.setContentIntent(pendingIntent).setAutoCancel(true)
//
//        with(NotificationManagerCompat.from(this)) {
//            notify(notificationId, newMessageNotification.build())
//        }
//    }

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
    // [END on_start_check_user]

    private fun signIn() {
//        Log.i(TAG, "Signing in")
//        sendNotification()
//        FirebaseMessaging.getInstance().unsubscribeFromTopic("test")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun viewUsersList() {
//        Log.i(TAG,"Moving on to UsersListActivity")
        val intent = Intent(applicationContext, UsersListActivity::class.java)
        startActivity(intent)
    }

    private fun addUser(user: FirebaseUser) {
        val userDetails = hashMapOf(
            "userId" to user.uid,
            "displayName" to user.displayName.toString(),
            "email" to user.email.toString(),
            "userImage" to user.photoUrl.toString(),
            "registrationId" to FirebaseInstanceId.getInstance().id
        )
        db.collection("users").document(userDetails["userId"].toString())
            .set(userDetails)
            .addOnSuccessListener { /*Log.i(TAG, "DocumentSnapshot successfully written!")*/ }
            .addOnFailureListener { /*exception -> Log.w(TAG, "Error writing document", exception)*/ }
    }

    private fun revokeAccess() {
        auth.signOut()

        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {

            welcomeText.text = "Hello and Welcome\n" + user.displayName

            //TODO: Set image to profile image
            Picasso.with(this)
                .load(user.photoUrl.toString())
                .transform(CircleTransform())
                .resize(100,100)
                .centerCrop()
                .into(imageView)

            emailText.text = "Email: " + user.email

            confirmationText.visibility = View.VISIBLE

            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
            orTextView.visibility = View.VISIBLE
            continueButton.visibility = View.VISIBLE
        } else {
            welcomeText.text = "Hello User\nSign In to continue"

            imageView.setImageResource(R.drawable.app_logo)
            emailText.text = ""

            confirmationText.visibility = View.GONE

            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
            orTextView.visibility = View.GONE
            continueButton.visibility = View.GONE
        }
    }

    // Start onActivityResult
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent();
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
//                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // SIgn in success, update UI with the signed-in user's information
//                    Log.d("Sign In Status", "success")
                    val user = auth.currentUser
                    updateUI(user)
                    addUser(user!!)
                } else {
                    // If sign in fails, display a message to the user
//                    Log.w("Sign In Status", "failure", task.exception)
                    Toast.makeText(this@LoginRegisterActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onClick(v: View) {
        val i = v.id
        Log.i(TAG, "Clicked " + v.id)
        when(i) {
            R.id.signInButton -> signIn()
            R.id.signOutButton -> signOut()
            R.id.continueButton -> viewUsersList()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 9001
        private const val CHANNEL_ID = "SimpleNotification"
        private const val notificationId = 9002
    }
}
