package com.emretaskesen.tpost.ui.activity.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.NotificationAdapter
import com.emretaskesen.tpost.databinding.ActivityNotificationPageBinding
import com.emretaskesen.tpost.model.NotificationModel
import com.emretaskesen.tpost.ui.fragment.user.FollowRequestFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationPage : AppCompatActivity(), NotificationAdapter.OnNotificationListener {
    lateinit var binding: ActivityNotificationPageBinding
    private lateinit var notificationsAdapter: NotificationAdapter
    private var notifyList = arrayListOf<NotificationModel>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {
        setupToolbar()
        setupRecyclerView()
        loadNotification()
        resetNotificationCount()

        binding.requestActionArea.setOnClickListener {
            val followRequestFragment = FollowRequestFragment()
            val tag = FollowRequestFragment::class.java.simpleName
            followRequestFragment.show(supportFragmentManager, tag)
        }
        onBackPressedDispatcher.addCallback(
            this /* lifecycle owner */,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    getFollowRequest()
                    finish()
                }
            })
    }

    private fun getFollowRequest(){
        val myID = auth.currentUser?.uid
        db.collection("Users").document(myID!!).collection("FollowRequest")
            .whereEqualTo("requestStatus", true).addSnapshotListener { snapshot, error ->
                if (error!=null){
                    return@addSnapshotListener
                }else{
                    if (snapshot!=null){
                        if (!snapshot.isEmpty){
                            val documents = snapshot.documents
                            val requestSize = documents.size
                            if (requestSize!=0){
                                binding.requestCounterText.text =
                                    getString(R.string.request_size_text, requestSize.toString())
                                binding.requestActionArea.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
    }



    private fun resetNotificationCount() {
        val myID = auth.currentUser?.uid ?: return
        db.collection("Users").document(myID).collection("Notification").whereEqualTo("isNew" , true)
            .get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                return@addOnSuccessListener
            }
            val batch = db.batch()

            // Update `readStatus` as part of batch operation for each document
            for (document in documents) {
                val docRef =
                    db.collection("Users").document(myID).collection("Notification")
                        .document(document.id)
                batch.update(docRef , "isNew" , false )
            }

            // Apply batch operation
            batch.commit().addOnCompleteListener {
                if (it.isSuccessful) {
                    return@addOnCompleteListener
                }
                else {
                    return@addOnCompleteListener
                }
            }
        }


    }


    private fun setupToolbar() {
        setSupportActionBar(binding.notificationToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initMenu()
    }

    private fun initMenu() {
        val menuHost : MenuHost = this
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_notification_page , menu)
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.delete_notifications ->{
                        true
                    }
                    android.R.id.home -> {
                        finish()
                        true
                    }


                    else -> false
                }
            }
        })
    }


    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        notificationsAdapter = NotificationAdapter(this, notifyList,this)
        binding.notificationList.layoutManager = layoutManager
        binding.notificationList.adapter = notificationsAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadNotification() {
        val myID = auth.currentUser?.uid
        db.collection("Users").document(myID!!).collection("Notification")
            .orderBy("notificationTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(applicationContext,"Bildirimler Alınamadı", Toast.LENGTH_LONG).show()
                } else {
                    if (snapshot != null) {
                        if (!snapshot.isEmpty) {
                            notifyList.clear()
                            val documents = snapshot.documents
                            for (ntfdoc in documents) {
                                val notificationID = ntfdoc.get("notificationID") as String?
                                val notificationTime = ntfdoc.get("notificationTime") as Timestamp?
                                val notificationContent =
                                    ntfdoc.get("notificationContent") as String?
                                val notificationTitle = ntfdoc.get("notificationTitle") as String?
                                val notificationType = ntfdoc.get("notificationType") as String?
                                val postID = ntfdoc.get("postID") as String?
                                val trackID = ntfdoc.get("trackID") as String?
                                val userID = ntfdoc.get("userID") as String?
                                if (trackID != myID) {
                                    val notifModel = NotificationModel(
                                        notificationID,
                                        notificationTime,
                                        notificationContent,
                                        notificationTitle,
                                        notificationType,
                                        postID,
                                        trackID,
                                        userID
                                    )
                                    notifyList.add(notifModel)
                                    getFollowRequest()
                                }

                            }
                            notificationsAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }

    }

    override fun onNotificationClick(notification: NotificationModel , view: View) {
        val content = view.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.followntfContent).text.toString()
        if (content=="Takip İsteği"){
            val followRequestFragment = FollowRequestFragment()
            val tag = FollowRequestFragment::class.java.simpleName
            followRequestFragment.show(supportFragmentManager, tag)
        }else{
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("userID", notification.trackID)
            startActivity(intent)
        }

    }
}

