package com.emretaskesen.tpost.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView

class RequestAdapter(
    val context : Context ,
    private var requestList : ArrayList<UserModel> ,
    private val listener : OnRequestListener ,
) : RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    val db = Firebase.firestore
    val auth = Firebase.auth

    interface OnRequestListener {
        fun onRequestClick(request : UserModel , view : View)
    }

    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val rvProfileImageV : CircleImageView = itemView.findViewById(R.id.rvProfileImage)
        val rvPersonalNameV : TextView = itemView.findViewById(R.id.rvPersonalName)
        val rvUserNameV : TextView = itemView.findViewById(R.id.rvUserName)
        val rvApproveRequestV : ImageButton = itemView.findViewById(R.id.rvApproveRequest)
        val rvDeleteRequestV : ImageButton = itemView.findViewById(R.id.rvDeleteRequest)
    }

    override fun onCreateViewHolder(parent : ViewGroup , viewType : Int) : ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_request_single , parent , false)
        return ViewHolder(view)
    }

    override fun getItemCount() : Int {
        return requestList.size
    }

    override fun onBindViewHolder(holder : ViewHolder , position : Int) {
        val currentRq = requestList[position]
        val myID = auth.currentUser?.uid
        UserCache.getUser(currentRq.userID !!) { userModel ->
            userModel?.let { user -> // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                holder.rvPersonalNameV.text =
                    user.personalName ?: context.getString(R.string.anonymous_user)
                holder.rvUserNameV.text = context.getString(R.string.user_name_tag , user.userName)
                Glide.with(context).load(user.profileImage).error(R.drawable.ic_rounded_user)
                    .into(holder.rvProfileImageV)
            } ?: run {
                UserCache.clear()
                return@getUser
            }
        }
        holder.rvDeleteRequestV.setOnClickListener {
            listener.onRequestClick(requestList[position] , holder.itemView)
            val uidFollower = db.collection("Users").document(myID !!).collection("FollowRequest")
                .document(currentRq.userID !!)
            uidFollower.delete().addOnSuccessListener {}.addOnFailureListener {}
        }
        holder.rvApproveRequestV.setOnClickListener {
            listener.onRequestClick(requestList[position] , holder.itemView)
            val followers = HashMap<String , Any>()
            followers["userID"] = currentRq.userID !!
            val uidFollower = db.collection("Users").document(myID !!).collection("Follower")
                .document(currentRq.userID !!)
            val uidFollowR = db.collection("Users").document(myID).collection("FollowRequest")
                .document(currentRq.userID !!)
            val follows = HashMap<String , Any>()
            follows["userID"] = myID
            val myuidFollow =
                db.collection("Users").document(currentRq.userID !!).collection("Follows")
                    .document(myID)
            uidFollowR.delete().addOnSuccessListener {
                uidFollower.set(followers).addOnSuccessListener {
                    myuidFollow.set(follows).addOnSuccessListener {
                        updateNotification(currentRq.userID !! , holder.rvUserNameV.text.toString())
                    }
                }
            }
        }

    }

    private fun updateNotification(userID : String , userName : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser !!
        val myID = currentUser.uid
        val notificationRef =
            db.collection("Users").document(myID).collection("Notification").document(userID)
        val notification = HashMap<String , Any>()
        notification["notificationID"] = userID
        notification["notificationType"] = "followNotification"
        notification["notificationTitle"] = "Yeni bir takipçin var"
        notification["notificationContent"] = "@$userName seni takip etti"
        notification["notificationTime"] = Timestamp.now()
        notification["trackID"] = userID
        notification["trackName"]
        notificationRef.set(notification).addOnSuccessListener {
            createNotification(userID)
        }
    }

    private fun createNotification(userID : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser !!
        val myID = currentUser.uid
        val myName = currentUser.displayName
        val notificationRef = db.collection("Users").document(userID).collection("Notification")
        val notification = HashMap<String , Any>()
        notification["notificationID"] = myID
        notification["notificationType"] = "followNotification"
        notification["notificationTitle"] = "@$myName Takip isteğini kabul etti"
        notification["notificationContent"] = "Hadi kontrol edelim"
        notification["notificationTime"] = Timestamp.now()
        notification["isNew"] = true
        notification["trackID"] = myID
        notificationRef.document(myID).set(notification)
    }


}
