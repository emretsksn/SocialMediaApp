package com.emretaskesen.tpost.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.Post
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.emretaskesen.tpost.ui.fragment.post.LikeFragment
import com.emretaskesen.tpost.ui.activity.post.PostDetail
import com.emretaskesen.tpost.ui.fragment.post.PostOptionFragment
import com.emretaskesen.tpost.ui.fragment.post.PostOptionUFragment
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import pl.droidsonroids.gif.GifImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PostAdapter(
    val context: Context ,
    private var postList: List<Post> ,
) :
    RecyclerView.Adapter<PostAdapter.PostHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPostList: List<Post>) {
          this.postList = newPostList as ArrayList<Post>
          notifyDataSetChanged()
      }
    val db = Firebase.firestore

    inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: CircleImageView = itemView.findViewById(R.id.vpUserProfile)
        val personalNameV: MaterialTextView = itemView.findViewById(R.id.vpPersonalName)
        val userNameV: MaterialTextView = itemView.findViewById(R.id.vpUserName)
        val postContentView: MaterialTextView = itemView.findViewById(R.id.vpPostContent)
        val postTimeView: MaterialTextView = itemView.findViewById(R.id.vpPostTime)
        val optionMenuView: ImageButton = itemView.findViewById(R.id.vpOption)
        val goToProfileV: ConstraintLayout = itemView.findViewById(R.id.vpGoToProfile)
        val likeButtonV: ImageButton = itemView.findViewById(R.id.vpLikeButton)
        val commentButtonV: ImageButton = itemView.findViewById(R.id.vpCommentButton)
        val likeSizeV: MaterialTextView = itemView.findViewById(R.id.vpLikeSize)
        val vpCommentSizeV: MaterialTextView = itemView.findViewById(R.id.vpCommentSize)
        val vpTaggedUsers: ChipGroup = itemView.findViewById(R.id.taggedUsers)
        val vpTagSelector: ImageButton = itemView.findViewById(R.id.tagSelector)
        val vpImageAreas: LinearLayout = itemView.findViewById(R.id.imagesArea)
        val vpLocationV: MaterialTextView = itemView.findViewById(R.id.vpLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_post_single, parent, false)
        return PostHolder(view)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder , position: Int) {
        val currentPost = postList[position]
        val myID = Firebase.auth.currentUser!!.uid
        var isLiked = false
        val taggedUserIDs = currentPost.taggedUserIDs
        val postImagesUri = currentPost.postImages
        when {
            !taggedUserIDs.isNullOrEmpty() -> {
                holder.vpTagSelector.visibility = View.VISIBLE
            }
            else -> {
                holder.vpTagSelector.visibility = View.GONE
            }
        }

        holder.vpImageAreas.removeAllViews()
        postImagesUri?.forEach { imageUri->
            val imagesArea = holder.vpImageAreas
            val imageWidth = 72.dpToPx()
            val imageHeight = 72.dpToPx()
            val images = GifImageView(context).apply {
                Glide.with(this).load(imageUri).into(this)
                layoutParams = ViewGroup.LayoutParams(imageWidth, imageHeight)
                setOnClickListener{
                    val showImageFragment =
                        ShowImageFragment.newInstance(imageUri.toUri())
                    // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                    showImageFragment.show(
                        (it.context as FragmentActivity).supportFragmentManager,
                        showImageFragment.tag
                    )
                }
            }

            imagesArea.addView(images)
        }


        holder.vpTaggedUsers.removeAllViews()
        taggedUserIDs?.forEach { userID ->
            val chip = Chip(context).apply {
                UserCache.getUser(userID) { userModel ->
                    userModel?.let { user ->
                        text = context.getString(R.string.user_name_tag, user.userName)
                    }
                }
                textSize = 11F
                setTextColor(context.getColorStateList(R.color.text_color))
                backgroundTintList = context.getColorStateList(R.color.button_background)
                chipBackgroundColor = context.getColorStateList(R.color.button_background)
                setOnClickListener {
                    if (userID == myID){
                        val bottomNavigationView = (context as FragmentActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                        bottomNavigationView.selectedItemId=R.id.userProfileFragment
                    }else{
                        val intent = Intent(context, UserProfile::class.java).apply {
                            putExtra("userID", userID)
                        }
                        context.startActivity(intent)
                    }

                }
            }
            holder.vpTaggedUsers.addView(chip) // Chip'i ChipGroup'a ekleyin
        }

        if (!currentPost.postLocation.isNullOrEmpty()){
            val latitude = currentPost.postLocation !!["latitude"]
            val longitude = currentPost.postLocation !!["longitude"]
            holder.vpLocationV.visibility = View.VISIBLE
            holder.vpLocationV.text = currentPost.locationName
            holder.vpLocationV.setOnClickListener {
                val label = currentPost.locationName
                val uriString = "geo:0,0?q=$latitude,$longitude($label)"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                intent.setPackage("com.google.android.apps.maps")
                context.startActivity(intent)
            }
        }else{
            holder.vpLocationV.visibility = View.GONE
        }
        // onBindViewHolder içinde kullanıcı bilgilerini ayarlamak için:
        UserCache.getUser(currentPost.userID!!) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                holder.personalNameV.text = user.personalName ?: context.getString(R.string.anonymous_user)
                holder.userNameV.text = context.getString(R.string.user_name_tag, user.userName)
                Glide.with(holder.itemView)
                    .load(user.profileImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.ic_rounded_user)
                    .into(holder.profileImageView)
            } ?: run {
                UserCache.clear()
                return@getUser
            }
        }
        holder.postContentView.text = currentPost.postContent


        val postTime = currentPost.postDate?.let { formatTimestamp(it) }
        holder.postTimeView.text = postTime

        db.collection("Post").document(currentPost.postID!!).collection("Like").addSnapshotListener { value, error ->
            if (error!=null){
                return@addSnapshotListener
            }else{
                if (value!=null){
                    if (!value.isEmpty){
                        when (val likeSize = value.documents.size) {
                            0 -> {
                                holder.likeSizeV.visibility = View.GONE
                            }
                            else -> {
                                holder.likeSizeV.visibility = View.VISIBLE
                                holder.likeSizeV.text =
                                    context.getString(R.string.comment_size , likeSize.toString())
                            }
                        }
                    }else{
                        holder.likeSizeV.visibility = View.GONE
                    }
                }
            }
        }

        db.collection("Post").document(currentPost.postID!!).collection("Comment").addSnapshotListener { value, error ->
            if (error == null) {
                if (value!=null){
                    if (value.isEmpty) {
                        val commentSize = 0
                        holder.vpCommentSizeV.text = commentSize.toString()
                    } else {
                        val commentSize = value.documents.size
                        holder.vpCommentSizeV.text = commentSize.toString()
                    }
                }
            } else {
                return@addSnapshotListener
            }
        }

        val likeDocRef = db.collection("Users").document(myID).collection("LikePost").document(currentPost.postID!!)

        likeDocRef.addSnapshotListener { value, error ->
            if (error == null) {
                if (value!=null){
                    when (value.get("postID") as String?) {
                        currentPost.postID -> {
                            isLiked = true
                            holder.likeButtonV.setImageResource(R.drawable.ic_like_full)
                        }
                        else -> {
                            isLiked = false
                            holder.likeButtonV.setImageResource(R.drawable.ic_like_null)
                        }
                    }
                }
            } else {
                return@addSnapshotListener
            }
        }

        // Butonların aksiyonları

        holder.optionMenuView.setOnClickListener {
            if (currentPost.userID == myID) {
                val postOptionUFragment =
                    PostOptionUFragment.newInstance(
                        currentPost.postID!!,
                        myID,
                        currentPost.postImages,
                        1
                    )
                postOptionUFragment.show(
                    (it.context as FragmentActivity).supportFragmentManager,
                    postOptionUFragment.tag
                )
            } else {
                val postOptionFragment =
                    PostOptionFragment.newInstance(currentPost.postID!!, currentPost.userID!!,1)
                postOptionFragment.show(
                    (it.context as FragmentActivity).supportFragmentManager,
                    postOptionFragment.tag
                )
            }

        }

        holder.goToProfileV.setOnClickListener {
            if (currentPost.userID == myID){
                val bottomNavigationView = (context as FragmentActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNavigationView.selectedItemId=R.id.userProfileFragment
            }else{
                val intent = Intent(context , UserProfile::class.java).apply {
                    putExtra("userID" , currentPost.userID)
                }
                context.startActivity(intent)
            }

        }




        holder.likeButtonV.setOnClickListener {
            if (isLiked) {
                // Beğeniyi kaldırma işlemi

                db.collection("Post").document(currentPost.postID !!).collection("Like")
                    .document(myID).delete().addOnSuccessListener {
                        db.collection("Users").document(myID).collection("LikePost")
                            .document(currentPost.postID!!).delete().addOnSuccessListener {
                            db.collection("Post").document(currentPost.postID !!).collection("Like")
                                .get().addOnSuccessListener { task ->
                                    val commentSize = task.size()
                                    holder.likeSizeV.text = context.getString(
                                        R.string.comment_size , commentSize.toString()
                                    )
                                    deleteNotification(myID , currentPost.postID !!)
                                    holder.likeButtonV.setImageResource(R.drawable.ic_like_null)
                                    isLiked = false
                                }

                        }
                    }
            }
            else {
                // Beğeni ekleme işlemi

                val likeMap = HashMap<String , Any>()
                likeMap["userID"] = myID
                likeMap["likeTime"] = Timestamp.now()

                db.collection("Post").document(currentPost.postID !!).collection("Like")
                    .document(myID).set(likeMap).addOnSuccessListener {
                        val likeSMap = HashMap<String , Any>()
                        likeSMap["likeTime"] = Timestamp.now()
                        likeSMap["postID"] = currentPost.postID !!
                        likeSMap["userID"] = currentPost.userID !!
                        db.collection("Users").document(myID).collection("LikePost")
                            .document(currentPost.postID!!).set(likeSMap).addOnSuccessListener {
                            db.collection("Post").document(currentPost.postID !!).collection("Like")
                                .get().addOnSuccessListener { task ->
                                    val commentSize = task.size()
                                    createNotification(
                                        currentPost.userID !! ,
                                        currentPost.postID !! ,
                                        commentSize.toString()
                                    )
                                    holder.likeSizeV.text = context.getString(
                                        R.string.comment_size , commentSize.toString()
                                    )
                                    holder.likeButtonV.setImageResource(R.drawable.ic_like_full)
                                    isLiked = true
                                }

                        }

                    }
            }
        }

        holder.commentButtonV.setOnClickListener {
            val intent = Intent(context, PostDetail::class.java).apply {
                putExtra("postID",currentPost.postID)
                putExtra("userID",currentPost.userID)
            }
            context.startActivity(intent)
        }
        holder.likeSizeV.setOnClickListener {
            val commentsFragment = LikeFragment.newInstance(currentPost.postID!!)
            // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
            commentsFragment.show(
                (it.context as FragmentActivity).supportFragmentManager,
                commentsFragment.tag
            )
        }
        holder.vpTagSelector.setOnClickListener {
            when (holder.vpTaggedUsers.visibility) {
                View.GONE -> {
                    holder.vpTaggedUsers.visibility = View.VISIBLE
                }
                else -> {
                    holder.vpTaggedUsers.visibility = View.GONE
                }
            }
        }

    }
    private fun Int.dpToPx() : Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun createNotification(userID: String, postID: String, likeSize: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser!!
        val myID = currentUser.uid
        val myName = currentUser.displayName
        val notificationRef = db.collection("Users").document(userID).collection("Notification")
        val notification = HashMap<String, Any>()
        notification["notificationID"] = myID
        notification["notificationType"] = "likeNotification"
        notification["notificationTitle"] = "@$myName paylaşımını beğendi."
        notification["notificationContent"] = "Paylaşımın toplam $likeSize beğeni aldı"
        notification["postID"] = postID
        notification["userID"] = userID
        notification["isNew"] = true
        notification["notificationTime"] = Timestamp.now()
        notification["trackID"] = myID
        notificationRef.document(myID).set(notification)
    }

    private fun deleteNotification(userID: String, postID: String) {
        val notifRef =
            db.collection("Users").document(userID).collection("Notification").document(postID)
        notifRef.delete()
    }


    private fun formatTimestamp(timestamp: Timestamp): String {
        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        val diffInMillis = now.timeInMillis - timestampDate.timeInMillis

        // Öncelikli olarak dakika içindeyse
        if (diffInMillis < 60 * 60 * 1000) {
            val minutes = (diffInMillis / (60 * 1000)).toInt()
            return if (minutes <= 0) context.getString(R.string.just_now) else context.getString(
                R.string.minute_ago,
                minutes.toString())
        }

        // Tarih kontrolü yaparak bugün veya dün için farklı formatlama
        val dateFormat: SimpleDateFormat = when {
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) ->
                SimpleDateFormat("HH:mm", Locale.getDefault())

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 ->
                SimpleDateFormat("'Dün' HH:mm", Locale.getDefault())

            else ->
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        }

        return dateFormat.format(timestampDate.time)
    }
}