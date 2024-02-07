package com.emretaskesen.tpost.ui.activity.post

import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityEditPostBinding
import com.emretaskesen.tpost.model.Post
import com.emretaskesen.tpost.ui.activity.main.MainActivity
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.emretaskesen.tpost.ui.fragment.user.AddUserFragment
import com.emretaskesen.tpost.util.ConstVal
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import pl.droidsonroids.gif.GifImageView
import java.util.UUID

class EditPost : AppCompatActivity(),
    AddUserFragment.BottomSheetListener {
    lateinit var binding : ActivityEditPostBinding
    val db = com.google.firebase.ktx.Firebase.firestore
    lateinit var auth : FirebaseAuth
    private val selectedUserIDs = mutableListOf<String>()
    private var documentReference: DocumentReference?=null
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        initViewsAndFunctions()
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedUserIDs.clear()
    }

    private fun initViewsAndFunctions() {
        setSupportActionBar(binding.postpageToolbar)
        val myActionBar : ActionBar? = supportActionBar
        myActionBar !!.setDisplayHomeAsUpEnabled(true)

        binding.addUserTagButton.setOnClickListener {
            val addUserFragment = AddUserFragment()
            addUserFragment.setBottomSheetListener(this)
            val tag = AddUserFragment::class.java.simpleName
            addUserFragment.show(supportFragmentManager , tag)
        }
        binding.sharePost.setOnClickListener {
            saveToDataBase()
            startActivity(Intent(this , MainActivity::class.java))
        }

        Glide.with(this@EditPost).load(auth.currentUser?.photoUrl).error(R.drawable.ic_rounded_user)
            .into(binding.userProfile)
        onBackPressedDispatcher.addCallback(this , object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()

            }
        })

        val postID = intent.getStringExtra("postID")
        postID?.let {
            documentReference = db.collection("Post").document(postID)
            getData()
        }
    }

    private fun getData() {
        documentReference!!.get().addOnSuccessListener { task ->
            val post = task.toObject(Post::class.java)
            post?.let {
                binding.postContent.setText(post.postContent)
                post.postImages?.forEach { imageUri ->
                    addImage(imageUri.toUri())
                }
                post.taggedUserIDs?.forEach { users ->
                    db.collection("Users").document(users).get().addOnSuccessListener { task ->
                        val userName = task.getString("userName") as String
                        val userID = task.getString("userID") as String
                        selectedUserIDs.add(userID)
                        addUserChip(userName, userID)
                    }
                }
                if (!post.postLocation.isNullOrEmpty()){
                    val latitude = post.postLocation !!["latitude"]
                    val longitude = post.postLocation !!["longitude"]
                    binding.addedLocation.text = post.locationName
                    binding.addedLocation.visibility = View.VISIBLE
                    binding.addedLocation.setOnClickListener {
                        val label = post.locationName
                        val uriString = "geo:0,0?q=$latitude,$longitude($label)"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                        intent.setPackage("com.google.android.apps.maps")
                        startActivity(intent)
                    }
                }else{
                    binding.addedLocation.visibility = View.GONE
                }

            }

        }
    }


    override fun onOptionsItemSelected(item : MenuItem) : Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun saveToDataBase() {
        val postContent = binding.postContent.text.toString()
        val currentTime = Timestamp.now()
        val postMap = hashMapOf<String , Any>()
        postMap["postDate"] = currentTime
        if (selectedUserIDs.isNotEmpty()){
            postMap["taggedUserIDs"] =  selectedUserIDs
        }
        if (postContent.isNotBlank()) {
            postMap["postContent"] = postContent
        }
        val postID = intent.getStringExtra("postID")
        documentReference!!.update(postMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                selectedUserIDs.forEach { userID ->
                    createNotification(postID!! , userID)
                }
                Toast.makeText(applicationContext ,
                    R.string.share_post_snackbar_message ,
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                applicationContext,
                exception.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
        }


    }

    private fun createNotification(postID :String , userID:String) {
        val myID = auth.currentUser!!.uid
        val notificationRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val documentPath = UUID.randomUUID().toString()
        val notificationMap = HashMap<String,Any>()
        notificationMap["notificationID"] = documentPath
        notificationMap["notificationType"] = "tagNotification"
        notificationMap["notificationTitle"] = "$myName sizi bir gönderiye etiketledi."
        notificationMap["notificationContent"] = binding.postContent.text.toString()
        notificationMap["postID"] = postID
        notificationMap["userID"] = myID
        notificationMap["notificationTime"] = Timestamp.now()
        notificationMap["trackID"] = myID
        notificationMap["isNew"] = true
        notificationRef.document(documentPath).set(notificationMap).addOnSuccessListener {
            db.collection("Users").document(userID).collection("Follower").get()
                .addOnSuccessListener {
                    sentFinishNotify()
                    this.finish()
                }
        }
    }
    private fun sentFinishNotify() {

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this,
            ConstVal.Notifications.CHANNEL_ID)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_all_post)
            .setContentTitle("Gönderi Paylaşıldı")
            .setSound(defaultSoundUri)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder)
    }


    override fun onBottomSheetResult(userID : String , userName : String) {
        if (selectedUserIDs.size < 5) {
            selectedUserIDs.add(userID)
            addUserChip(userName , userID)
        } else { // Liste dolu olduğunda yapılacak işlemler (örneğin kullanıcıyı bilgilendirme)
            Toast.makeText(this , "En fazla 5 kullanıcı eklenebilir." , Toast.LENGTH_SHORT)
                .show()
        }


    }

    private fun addImage(imageUrl : Uri){
        val imagesArea = binding.imagesArea
        val imageWidth = 72.dpToPx()
        val imageHeight = 72.dpToPx()
        val images = GifImageView(this).apply {
            Glide.with(this).load(imageUrl).into(this)
            layoutParams = ViewGroup.LayoutParams(imageWidth, imageHeight)
            setOnClickListener{
                val showImageFragment =
                    ShowImageFragment.newInstance(imageUrl)
                // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                showImageFragment.show(
                    (it.context as FragmentActivity).supportFragmentManager,
                    showImageFragment.tag
                )
            }
        }

        imagesArea.addView(images)
    }

    private fun Int.dpToPx() : Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
    private fun addUserChip(userName : String , userID : String) {
        val icon : Drawable? = ContextCompat.getDrawable(this@EditPost , R.drawable.ic_notif_ment)
        val chipGroup = binding.taggedUsers
        val chip : Chip = Chip(this).apply {
            text = userName
            isCloseIconVisible = true
            isChipIconVisible = true
            textSize = 11F
            chipIcon = icon
            chipBackgroundColor =
                getColorStateList(R.color.card_background) // Chip'i kapatma (silme) işlemi için listener
            setOnCloseIconClickListener {
                chipGroup.removeView(this)
                selectedUserIDs.remove(userID) // Bu ID'yi listenden çıkar (userID'yi elde etmeniz gerekecek)
            }
        }
        chipGroup.addView(chip)
    }

}


