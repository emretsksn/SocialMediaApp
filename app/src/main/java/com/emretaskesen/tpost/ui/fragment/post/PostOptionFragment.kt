package com.emretaskesen.tpost.ui.fragment.post

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentPostOptionBinding
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.emretaskesen.tpost.ui.activity.post.PostDetail
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class PostOptionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPostOptionBinding
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(postID: String, userID: String,callerIntent : Int): PostOptionFragment {
            val fragment = PostOptionFragment()
            val args = Bundle()
            args.putString("postID",postID)
            args.putString("userID",userID)
            args.putInt("callerIntent",callerIntent)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPostOptionBinding.inflate(inflater, container, false)
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {
        val postID : String? = arguments?.getString("postID")
        val userID : String? = arguments?.getString("userID")
        val callerIntent : Int? = arguments?.getInt("callerIntent")
        val myID = auth.currentUser?.uid
        var isFollow = false
        var isSave = false
        var sendRequest = false
        val drawableLeft1: Drawable? = ContextCompat.getDrawable(requireActivity() , R.drawable.ic_user_add)
        val drawableLeft2: Drawable? = ContextCompat.getDrawable(requireActivity(),R.drawable.ic_user_remover)
        val drawableLeft3: Drawable? = ContextCompat.getDrawable(requireActivity(),R.drawable.ic_bookmark_add)
        val drawableLeft4: Drawable? = ContextCompat.getDrawable(requireActivity(),R.drawable.ic_bookmark_remove)

        userID?.let {
            db.collection("Users").document(userID).get().addOnSuccessListener { task ->
               val accountStatus= task.get("accountStatus") as Boolean?
                if ((accountStatus == true) && ! isFollow){
                    binding.pmFollowuser.visibility = View.GONE
                    binding.pmSendRequest.visibility = View.VISIBLE
                }else{
                    binding.pmSendRequest.visibility = View.GONE
                    binding.pmFollowuser.visibility = View.VISIBLE
                }
            }
            db.collection("Users").document(userID).collection("FollowRequest")
                .addSnapshotListener { getData , e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    else {
                        if (getData != null) {
                            if (! getData.isEmpty) {
                                val documents = getData.documents
                                for (document in documents) {
                                    val followerID = document.get("userID") as String?
                                    if (followerID == myID) {
                                        sendRequest = true
                                        binding.pmSendRequest.setText(R.string.request_completed)
                                        binding.pmSendRequest.setCompoundDrawablesWithIntrinsicBounds(
                                            drawableLeft2,  // Sol (start) drawable
                                            null,       // Üst drawable
                                            null,       // Sağ (end) drawable
                                            null        // Alt drawable
                                        )
                                    }
                                    else {
                                        binding.pmSendRequest.setText(R.string.title_followuser)
                                        sendRequest = false
                                        binding.pmSendRequest.setCompoundDrawablesWithIntrinsicBounds(
                                            drawableLeft1,  // Sol (start) drawable
                                            null,       // Üst drawable
                                            null,       // Sağ (end) drawable
                                            null        // Alt drawable
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

        }


        val followRef = db.collection("Users").document(myID!!).collection("Follows").document(userID!!)
        followRef.addSnapshotListener { value, error ->
            if (error!=null){
                return@addSnapshotListener
            }else{
                if (value!=null){
                    val followsId = value.get("userID") as String?
                    if (userID == followsId){
                        isFollow = true
                        binding.pmFollowuser.setText(R.string.unfollow)
                        binding.pmFollowuser.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft2,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }else{
                        isFollow = false
                        binding.pmFollowuser.setText(R.string.title_followuser)
                        binding.pmFollowuser.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft1,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }
                }
            }
        }

        val saveRef = db.collection("Users").document(myID).collection("SavePost").document(postID!!)
        saveRef.addSnapshotListener { value, error ->
            if (error!=null){
                return@addSnapshotListener
            }else{
                if (value!=null){
                    val postId = value.get("postID") as String?
                    if (postID == postId){
                        isSave = true
                        binding.pmSave.text = getString(R.string.save_comleted)
                        binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft4,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }else{
                        isSave = false
                        binding.pmSave.text = getString(R.string.save)
                        binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft3,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }
                }
            }
        }

        if (callerIntent==2){
            binding.pmView.visibility = View.GONE
        }else{
            binding.pmView.visibility = View.VISIBLE
        }
        binding.pmView.setOnClickListener {
            dismiss()
            val intent =
                Intent(requireActivity(), PostDetail::class.java)
            intent.putExtra("postID", postID)
            intent.putExtra("userID", userID)
            requireActivity().startActivity(intent)
        }

        if (isFollow){
            binding.pmSendmessage.visibility = View.VISIBLE
        }else{
            binding.pmSendmessage.visibility = View.GONE
        }

        binding.pmSendmessage.setOnClickListener {
            dismiss()
            val intent = Intent(context, ChatPage::class.java)
            intent.putExtra("userID", userID)
            requireActivity().startActivity(intent)
        }
        binding.pmFollowuser.setOnClickListener {
            if (isFollow){
                val uidFollower =
                        db.collection("Users").document(userID).collection("Follower")
                            .document(myID)
                val myUidFollow = db.collection("Users").document(myID).collection("Follows")
                    .document(userID)
                uidFollower.delete().addOnSuccessListener {
                    myUidFollow.delete().addOnSuccessListener {
                        deleteNotification()
                        isFollow = false
                        binding.pmFollowuser.setText(R.string.title_followuser)
                        binding.pmFollowuser.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft1,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }
                }
            }else{
                val uidFollower =
                        db.collection("Users").document(userID).collection("Follower")
                            .document(myID)
                val myUidFollow = db.collection("Users").document(myID).collection("Follows")
                    .document(userID)
                val followers = HashMap<String, Any>()
                followers["userID"] = myID
                val follows = HashMap<String, Any>()
                follows["userID"] = userID
                uidFollower.set(followers).addOnSuccessListener {
                    myUidFollow.set(follows).addOnSuccessListener {
                        createNotification()
                        isFollow = true
                        binding.pmFollowuser.setText(R.string.unfollow)
                        binding.pmFollowuser.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft2,  // Sol (start) drawable
                            null,       // Üst drawable
                            null,       // Sağ (end) drawable
                            null        // Alt drawable
                        )

                    }.addOnFailureListener { e ->
                        Toast.makeText(requireActivity(),e.localizedMessage,Toast.LENGTH_LONG).show()
                    }
                }

            }
            dismiss()
        }

        binding.pmSave.setOnClickListener {
            if (isSave){
                val myUidSave = db.collection("Users").document(myID).collection("SavePost")
                    .document(postID)
                myUidSave.delete().addOnSuccessListener {

                    isSave = false
                    binding.pmSave.text = getString(R.string.save)
                    binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft3,  // Sol (start) drawable
                        null,       // Üst drawable
                        null,       // Sağ (end) drawable
                        null        // Alt drawable
                    )
                }

            }else{
                val myUidSave = db.collection("Users").document(myID).collection("SavePost")
                val saveMap = HashMap<String , Any>()
                saveMap["likeTime"] = Timestamp.now()
                saveMap["postID"] = postID
                saveMap["userID"] = userID
                myUidSave.document(postID).set(saveMap).addOnSuccessListener {

                    isSave = true
                    binding.pmSave.text = getString(R.string.save_comleted)
                    binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft4,  // Sol (start) drawable
                        null,       // Üst drawable
                        null,       // Sağ (end) drawable
                        null        // Alt drawable
                    )
                }
            }
        }

        binding.pmGotoprofile.setOnClickListener {
            dismiss()
            val intent = Intent(context, UserProfile::class.java)
            intent.putExtra("userID", userID)
            requireActivity().startActivity(intent)
        }

        binding.pmSendRequest.setOnClickListener {
            if (sendRequest){
                deleteRequest()
            }else{
                sendRequest()
            }
        }
    }

    private fun sendRequest() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val followers = HashMap<String , Any>()
        followers["userID"] = myID
        followers["requestTime"] = Timestamp.now()
        val uidFollower =
            db.collection("Users").document(userID).collection("FollowRequest").document(myID)

        uidFollower.set(followers).addOnSuccessListener {
            binding.pmSendRequest.setText(R.string.request_completed)
        }
    }

    private fun deleteRequest() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val uidFollower =
            db.collection("Users").document(userID).collection("FollowRequest").document(myID)
        uidFollower.delete().addOnSuccessListener {
            binding.pmSendRequest.setText(R.string.title_followuser)
        }.addOnFailureListener {
            binding.pmSendRequest.setText(R.string.request_completed)
        }
    }

    private fun createRequestNotification() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val notifMap = HashMap<String , Any>()
        notifMap["notificationID"] = myID
        notifMap["notificationType"] = "followNotification"
        notifMap["notificationTitle"] = "Takip İsteği"
        notifMap["notificationContent"] = "@$myName seni takip etmek istiyor"
        notifMap["notificationTime"] = Timestamp.now()
        notifMap["trackID"] = myID
        notifRef.document(myID).set(notifMap).addOnSuccessListener {}
    }

    private fun deleteRequestNotification() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        notifRef.document(myID).delete().addOnSuccessListener {}
    }


    private fun createNotification() {
        val userID : String =
                arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val notifMap = HashMap<String , Any>()
        notifMap["notificationID"] = myID
        notifMap["notificationType"] = "followNotification"
        notifMap["notificationTitle"] = "Yeni bir takipçin var"
        notifMap["notificationContent"] = "@$myName seni takip etti"
        notifMap["notificationTime"] = Timestamp.now()
        notifMap["trackID"] = myID
        notifMap["isNew"] = true
        notifRef.document(myID).set(notifMap).addOnSuccessListener {
        }
    }

    private fun deleteNotification() {
        val userID : String =
                arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val notifRef =
                db.collection("Users").document(userID).collection("Notification").document(myID)
        notifRef.delete()
    }



}