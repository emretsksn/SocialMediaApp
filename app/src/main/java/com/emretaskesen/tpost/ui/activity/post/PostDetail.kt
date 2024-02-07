package com.emretaskesen.tpost.ui.activity.post

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.PostCache
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.ActivityPostDetailBinding
import com.emretaskesen.tpost.model.CommentModel
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.emretaskesen.tpost.ui.adapter.CommentAdapter
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.emretaskesen.tpost.ui.fragment.post.LikeFragment
import com.emretaskesen.tpost.ui.fragment.post.PostOptionFragment
import com.emretaskesen.tpost.ui.fragment.post.PostOptionUFragment
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import pl.droidsonroids.gif.GifImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class PostDetail : AppCompatActivity() , CommentAdapter.OnCommentListener {
    val db = Firebase.firestore
    private lateinit var auth : FirebaseAuth
    lateinit var binding : ActivityPostDetailBinding
    var postID : String? = null
    var userID : String? = null
    private var postImages : ArrayList<String>? = null
    private var isLiked = false
    lateinit var recyclerViewAdapter : CommentAdapter
    private var commentList = arrayListOf<CommentModel>()
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        postID = intent.getStringExtra("postID")
        userID = intent.getStringExtra("userID")
        if (postID == null) {
            finish()
        } else {
            initViewsAndFunctions()
        }
    }

    private fun initViewsAndFunctions() {
        initMenu()
        val postID = intent.getStringExtra("postID")
        val userID = intent.getStringExtra("userID") //recyclerview
        getFirebaseData()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.commentDetail.layoutManager = layoutManager
        recyclerViewAdapter = CommentAdapter(this , commentList , this)
        binding.commentDetail.adapter = recyclerViewAdapter

        //install settings


        auth = Firebase.auth
        onBackPressedDispatcher.addCallback(this , object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        setupToolbar()
        binding.vpGoToProfile.setOnClickListener {
            val otherUser = Intent(this , UserProfile::class.java)
            otherUser.putExtra("userID" , userID)
            startActivity(otherUser)
        }
        binding.vpCommentButton.setOnClickListener {
            if (binding.commentArea.visibility == View.GONE) {
                binding.commentArea.visibility = View.VISIBLE
            } else {
                binding.commentArea.visibility = View.GONE
            }

        }
        binding.vpLikeSize.setOnClickListener {
            val likeFragment = LikeFragment.newInstance(postID !!)
            val fragmentManager = supportFragmentManager
            val tag = LikeFragment::class.java.simpleName
            likeFragment.show(fragmentManager , tag)
        }
        binding.vpLikeButton.setOnClickListener {
            val myID = auth.currentUser !!.uid
            if (isLiked) { // Beğeniyi kaldırma işlemi
                binding.vpLikeButton.setImageResource(R.drawable.ic_like_null)
                db.collection("Post").document(postID !!).collection("Like").document(myID).delete()
                    .addOnSuccessListener {
                        db.collection("Post").document(postID).collection("Like").get()
                            .addOnSuccessListener { task ->
                                val commentSize = task.size()
                                binding.vpLikeSize.text =
                                    getString(R.string.comment_size , commentSize.toString())
                                deleteNotification(myID , postID)
                            }
                        isLiked = false
                    }
            } else { // Beğeni ekleme işlemi
                binding.vpLikeButton.setImageResource(R.drawable.ic_like_full)
                val likeMap = HashMap<String , Any>()
                likeMap["userID"] = myID
                likeMap["likeTime"] = Timestamp.now()
                db.collection("Post").document(postID !!).collection("Like").document(myID)
                    .set(likeMap).addOnSuccessListener {
                        db.collection("Post").document(postID).collection("Like").get()
                            .addOnSuccessListener { task ->
                                val commentSize = task.size()
                                createNotification(userID !! , postID , commentSize.toString())
                                binding.vpLikeSize.text =
                                    getString(R.string.comment_size , commentSize.toString())
                            }
                        isLiked = true
                    }
            }
        } //
        getComment(postID !!)
        binding.commentSent.setOnClickListener {
            val commentText = binding.commentText.text.toString()
            sendComment(postID , commentText , userID !!)
        }
        binding.tagSelector.setOnClickListener {
            when (binding.taggedUsers.visibility) {
                View.GONE -> {
                    binding.taggedUsers.visibility = View.VISIBLE
                }

                else -> {
                    binding.taggedUsers.visibility = View.GONE
                }
            }
        }

    }

    private fun sendComment(postID : String , comment : String , userID : String) {
        val myId = auth.currentUser !!.uid
        val documentID = UUID.randomUUID().toString()
        val commentMap = HashMap<String , Any>()
        commentMap["userID"] = myId
        commentMap["commentID"] = documentID
        commentMap["commentTime"] = Timestamp.now()
        commentMap["commentContent"] = comment
        db.collection("Post").document(postID).collection("Comment").document(documentID)
            .set(commentMap).addOnSuccessListener {
                createCommentNotification(postID , userID)
            }
    }

    private fun createCommentNotification(postID : String , userID : String) {
        val myID = auth.currentUser !!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val documentPath = UUID.randomUUID().toString()
        val notifMap = HashMap<String , Any>()
        notifMap["notificationID"] = documentPath
        notifMap["notificationType"] = "commentNotification"
        notifMap["notificationTitle"] = "@$myName paylaşımına yorum yaptı"
        notifMap["notificationContent"] = binding.commentText.text.toString()
        notifMap["postID"] = postID
        notifMap["userID"] = userID
        notifMap["notificationTime"] = Timestamp.now()
        notifMap["trackID"] = myID
        notifRef.document(documentPath).set(notifMap).addOnSuccessListener {
            db.collection("Users").document(userID).collection("Follower").get()
                .addOnSuccessListener {
                    binding.commentText.setText("")
                }
        }
    }

    private fun createNotification(userID : String , postID : String , likeSize : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser !!
        val myID = currentUser.uid
        val myName = currentUser.displayName
        val notificationRef = db.collection("Users").document(userID).collection("Notification")
        val notification = HashMap<String , Any>()
        notification["notificationID"] = myID
        notification["notificationType"] = "likeNotification"
        notification["notificationTitle"] = "@$myName paylaşımını beğendi."
        notification["notificationContent"] = "Paylaşımın toplam $likeSize beğeni aldı"
        notification["postID"] = postID
        notification["userID"] = userID
        notification["notificationTime"] = Timestamp.now()
        notification["trackID"] = myID
        notification["isNew"] = true
        notificationRef.document(myID).set(notification)
    }

    private fun deleteNotification(userID : String , postID : String) {
        val notifRef =
            db.collection("Users").document(userID).collection("Notification").document(postID)
        notifRef.delete()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.postDetailToolbar)
        val myActionBar : ActionBar? = supportActionBar
        myActionBar !!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initMenu() {
        val menuHost : MenuHost = this
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(
                menu : Menu ,
                menuInflater : MenuInflater ,
            ) { // Add menu items here
                menuInflater.inflate(R.menu.menu_post_detail , menu)
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean { // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.postSetting -> {
                        showAction()
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

    private fun showAction() {
        val myID = auth.currentUser !!.uid
        if (userID == myID) {
            val postOptionUFragment =
                PostOptionUFragment.newInstance(postID !! , myID , postImages , 2)
            postOptionUFragment.show(supportFragmentManager , postOptionUFragment.tag)
        } else {
            val postOptionFragment = PostOptionFragment.newInstance(postID !! , userID !! , 2)
            postOptionFragment.show(supportFragmentManager , postOptionFragment.tag)
        }
    }

    private fun getFirebaseData() {
        val postID = intent.getStringExtra("postID")
        val userID = intent.getStringExtra("userID")
        postID?.let {
            PostCache.getPost(postID) { postModel ->
                postModel?.let { posts ->
                    posts.postID?.let {
                        getLikeData(it)
                        commentData(it)
                        getLikeBigData(it)
                    }
                    val currentTime = posts.postDate?.let { formatTimestamp(it) }
                    binding.vpPostTime.text = currentTime
                    binding.vpPostContent.text = posts.postContent
                    when {
                        posts.taggedUserIDs.isNullOrEmpty() -> {
                            binding.tagSelector.visibility = View.GONE
                        }

                        else -> {
                            binding.tagSelector.visibility = View.VISIBLE
                        }
                    }
                    binding.taggedUsers.removeAllViews()
                    posts.taggedUserIDs?.forEach { userID ->
                        val chip = Chip(this).apply {
                            UserCache.getUser(userID) { userModel ->
                                userModel?.let { user ->
                                    text = context.getString(R.string.user_name_tag , user.userName)
                                }
                            }
                            textSize = 11F
                            setTextColor(context.getColorStateList(R.color.text_color))
                            backgroundTintList =
                                context.getColorStateList(R.color.button_background)
                            chipBackgroundColor =
                                context.getColorStateList(R.color.button_background)
                            setOnClickListener { // Kullanıcı profil sayfasına gitme niyeti
                                val intent = Intent(context , UserProfile::class.java).apply {
                                    putExtra("userID" , userID)
                                }
                                context.startActivity(intent)
                            }
                        }
                        binding.taggedUsers.addView(chip) // Chip'i ChipGroup'a ekleyin
                    }

                    postImages = posts.postImages

                    posts.postImages?.forEach { imageUri ->
                        val imagesArea = binding.imagesArea
                        val imageWidth = 72.dpToPx()
                        val imageHeight = 72.dpToPx()
                        val images = GifImageView(this).apply {
                            Glide.with(this).load(imageUri).into(this)
                            layoutParams = ViewGroup.LayoutParams(imageWidth , imageHeight)
                            setOnClickListener {
                                val showImageFragment =
                                    ShowImageFragment.newInstance(imageUri.toUri()) // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                                showImageFragment.show((it.context as FragmentActivity).supportFragmentManager ,
                                    showImageFragment.tag)
                            }
                        }

                        imagesArea.addView(images)
                    }
                } ?: run {
                    PostCache.clear()
                    return@getPost
                }
            }
        }

        userID?.let {
            UserCache.getUser(it) { userModel ->
                userModel?.let { user ->
                    binding.vpPersonalName.text =
                        user.personalName ?: getString(R.string.anonymous_user)
                    binding.vpUserName.text = getString(R.string.user_name_tag , user.userName)
                    Glide.with(this).load(user.profileImage).error(R.drawable.ic_rounded_user)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).into(binding.vpUserProfile)
                } ?: run {
                    UserCache.clear()
                    return@getUser
                }
            }
        }

    }

    private fun Int.dpToPx() : Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun getLikeData(postID : String) {
        db.collection("Post").document(postID).collection("Like")
            .addSnapshotListener { value , error ->
                if (error != null) {
                    return@addSnapshotListener
                } else {
                    if (value != null) {
                        if (! value.isEmpty) {
                            when (val likeSize = value.documents.size) {
                                0 -> {
                                    binding.vpLikeSize.visibility = View.GONE
                                }

                                else -> {
                                    binding.vpLikeSize.visibility = View.VISIBLE
                                    binding.vpLikeSize.text =
                                        getString(R.string.comment_size , likeSize.toString())
                                }
                            }
                        } else {
                            binding.vpLikeSize.visibility = View.GONE
                        }
                    }
                }
            }
    }

    private fun commentData(postID : String) {

        db.collection("Post").document(postID).collection("Comment").get()
            .addOnSuccessListener { task ->
                val commentSize = task.size().toString()
                binding.vpCommentSize.text = commentSize
            }
    }

    private fun getLikeBigData(postID : String) {
        val myID = auth.currentUser?.uid
        db.collection("Post").document(postID).collection("Like").whereEqualTo("userID" , myID)
            .get().addOnSuccessListener { task ->
                if (task != null) {
                    val documents = task.documents
                    for (likeDoc in documents) {
                        val userID = likeDoc.get("userID") as String
                        if (userID != myID) {
                            isLiked = false
                            binding.vpLikeButton.setImageResource(R.drawable.ic_like_null)
                        } else {
                            isLiked = true
                            binding.vpLikeButton.setImageResource(R.drawable.ic_like_full)
                        }
                    }

                } else {
                    return@addOnSuccessListener
                }
            }
    }

    private fun formatTimestamp(timestamp : Timestamp) : String {
        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        val diffInMillis = now.timeInMillis - timestampDate.timeInMillis

        // Öncelikli olarak dakika içindeyse
        if (diffInMillis < 60 * 60 * 1000) {
            val minutes = (diffInMillis / (60 * 1000)).toInt()
            return if (minutes <= 0) "az önce" else "$minutes dakika önce"
        }

        // Tarih kontrolü yaparak bugün veya dün için farklı formatlama
        val dateFormat : SimpleDateFormat = when {
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) -> SimpleDateFormat("HH:mm" ,
                Locale.getDefault())

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 -> SimpleDateFormat("'Dün' HH:mm" ,
                Locale.getDefault())

            else -> SimpleDateFormat("dd.MM.yyyy" , Locale.getDefault())
        }

        return dateFormat.format(timestampDate.time)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getComment(postID : String) {
        db.collection("Post").document(postID).collection("Comment")
            .orderBy("commentTime" , Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot , e ->
                if (e != null) {
                    return@addSnapshotListener
                } else {
                    if (snapshot != null) {
                        if (! snapshot.isEmpty) {
                            commentList.clear()
                            val documents = snapshot.documents
                            for (commentD in documents) {
                                val userID = commentD.get("userID") as String?
                                val commentID = commentD.get("commentID") as String?
                                val commentTime = commentD.get("commentTime") as Timestamp?
                                val commentContent = commentD.get("commentContent") as String?
                                val downloadComment = CommentModel(userID = userID ,
                                    commentID = commentID ,
                                    postID = postID ,
                                    commentContent = commentContent ,
                                    commentTime = commentTime)
                                commentList.add(downloadComment)
                            }
                            recyclerViewAdapter.notifyDataSetChanged()
                            binding.emptyText.visibility = View.VISIBLE
                        } else {
                            binding.emptyText.text = "Yorum Yok"
                            binding.emptyText.visibility = View.GONE
                        }

                    }

                }
            }

    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onCommentClick(comment : CommentModel , view : View) {
        db.collection("Post").document(comment.postID !!).collection("Comment")
            .document(comment.commentID !!).delete().addOnSuccessListener {
                Toast.makeText(this , "Yorum Silindi" , Toast.LENGTH_LONG).show()
                commentList.removeIf { it.commentID == comment.commentID  }
                recyclerViewAdapter.notifyDataSetChanged()
            }
    }

}