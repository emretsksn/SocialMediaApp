package com.emretaskesen.tpost.ui.fragment.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.HomeFragmentStateAdapter
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.FragmentOtherUserBinding
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.emretaskesen.tpost.ui.activity.user.FollowInfo
import com.emretaskesen.tpost.ui.fragment.post.MentionPostFragment
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class OtherUserFragment : Fragment() {

    // View'lara erişebilmek için binding nesnesi
    lateinit var binding : FragmentOtherUserBinding
    var auth = FirebaseAuth.getInstance()
    val db = com.google.firebase.Firebase.firestore
    var profileImage : String? = null
    var userName : String? = null
    private var isFollow : Boolean? = null
    private lateinit var fragmentManager : FragmentManager
    private lateinit var materialToolbar : MaterialToolbar

    // Adapter değişkeni
    private lateinit var adapter : HomeFragmentStateAdapter
    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View { // ui bağlantısı
        binding = FragmentOtherUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        materialToolbar = binding.userprofileToolbar
        isFollow = false
        val userID : String = arguments?.getString("userID")
            ?: throw IllegalStateException("UserID is required") // UserPost ve TaggedPost fragmentlarını yarat
        val userPostFragment = UserPostFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        }

        val mentionPostFragment = MentionPostFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        } // Fragment listesini yarat ve fragmentları ekle
        val fragmentList = arrayListOf(userPostFragment , mentionPostFragment)
        fragmentManager =
            requireActivity().supportFragmentManager // Adapteri yarat ve viewpager'a set et
        initViewPagerAdapter(fragmentList)
        initMenu()
        installSettings(userID)
        privateAccountData(userID)
        getRequestStatus(userID)
        getFollowData(userID)
        initViewsAndFunctions(userID)
    }

    private fun showComponent() {
        binding.privateAccountFollow.visibility = View.GONE
        binding.postViewArea.visibility = View.VISIBLE
        binding.publicAccountArea.visibility = View.VISIBLE
        binding.tabLayoutHomeFragment.visibility = View.VISIBLE
    }

    private fun hideComponent() {
        binding.privateAccountFollow.visibility = View.VISIBLE
        binding.postViewArea.visibility = View.GONE
        binding.publicAccountArea.visibility = View.GONE
        binding.tabLayoutHomeFragment.visibility = View.GONE
    }

    private fun initViewPagerAdapter(fragmentList : ArrayList<Fragment>) { // Adapter init
        val viewPager = binding.vpHome
        adapter = HomeFragmentStateAdapter(childFragmentManager ,
            viewLifecycleOwner.lifecycle ,
            fragmentList)
        viewPager.adapter = adapter

        // TabLayout ile ViewPager bağlantısı yapılır.
        TabLayoutMediator(binding.tabLayoutHomeFragment , viewPager) { tab , position ->
            when (position) {
                0 -> {
                    tab.setText(getString(R.string.shares))
                }

                1 -> {
                    tab.setText(getString(R.string.mentions))
                }
            }
        }.attach()
    }

    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean { // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        activity?.finish()
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }

    private fun initViewsAndFunctions(userID : String) {

        binding.myPhoto.setOnLongClickListener {

            var photoUri : Uri? = null
            UserCache.getUser(userID) { userModel ->
                photoUri = userModel?.profileImage?.toUri()
            }
            val showImageFragment =
                ShowImageFragment.newInstance(photoUri) // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
            showImageFragment.show(requireActivity().supportFragmentManager , showImageFragment.tag)
            true
        }

        binding.sendMessageButton.setOnClickListener {
            val intent = Intent(activity , ChatPage::class.java)
            intent.putExtra("userID" , userID)
            startActivity(intent)
        }
        binding.followButton.setOnClickListener {
            val buttonText = binding.followButton.text.toString()
            if (buttonText.contentEquals(getString(R.string.title_followuser))) {
                createFollower()
            }
            if (buttonText.contentEquals(getString(R.string.unfollow))) {
                createUnFollower()
            }

        }
        binding.followCount.setOnClickListener {
            val intent = Intent(requireActivity(), FollowInfo::class.java).apply {
                putExtra("userID" , userID)
            }
            requireActivity().startActivity(intent)
        }
        binding.followerCount.setOnClickListener {
            val intent = Intent(requireActivity(), FollowInfo::class.java).apply {
                putExtra("userID" , userID)
            }
            requireActivity().startActivity(intent)
        }
        binding.privateAccountFollow.setOnClickListener {
            if (binding.privateAccountFollow.text == getString(R.string.title_followuser)) {
                sendRequest()
                createRequestNotification()
            } else if (binding.privateAccountFollow.text == getString(R.string.request_completed)) {
                deleteRequestNotification()
                deleteRequest()
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
            binding.privateAccountFollow.setText(R.string.request_completed)
        }
    }

    private fun deleteRequest() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val uidFollower =
            db.collection("Users").document(userID).collection("FollowRequest").document(myID)
        uidFollower.delete().addOnSuccessListener {
            binding.privateAccountFollow.setText(R.string.title_followuser)
        }.addOnFailureListener {
            binding.privateAccountFollow.setText(R.string.request_completed)
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

    private fun createFollower() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val followers = HashMap<String , Any>()
        followers["userID"] = myID
        val uidFollower =
            db.collection("Users").document(userID).collection("Follower").document(myID)

        uidFollower.set(followers).addOnSuccessListener {
            createFollow()
        }
    }

    private fun createFollow() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val follows = HashMap<String , Any>()
        follows["followsId"] = userID
        val myuidFollow =
            db.collection("Users").document(myID).collection("Follows").document(userID)
        myuidFollow.set(follows).addOnSuccessListener {
            createNotification()
            binding.followButton.setText(R.string.unfollow)
        }.addOnFailureListener {
            Toast.makeText(requireActivity() ,
                "Beklenmedik bir hata oluştu. Tekrar deneyiniz." ,
                Toast.LENGTH_LONG).show()
        }
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
            db.collection("Users").document(userID).collection("Follower").get()
                .addOnSuccessListener { task ->
                    binding.followerCount.text = task.size().toString()

                }
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

    private fun createUnFollower() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val uidFollower =
            db.collection("Users").document(userID).collection("Follower").document(myID)

        uidFollower.delete().addOnSuccessListener {
            createUnFollow()
        }
    }

    private fun createUnFollow() {
        val userID : String =
            arguments?.getString("userID") ?: throw IllegalStateException("UserID is required")
        val myID = auth.currentUser !!.uid
        val myUidFollow =
            db.collection("Users").document(myID).collection("Follows").document(userID)

        myUidFollow.delete().addOnSuccessListener {
            binding.followButton.setText(R.string.title_followuser)
            deleteNotification()
            requireActivity().recreate()
        }
    }

    private fun getFollowData(userID : String) {
        val myID = auth.currentUser?.uid
        db.collection("Users").document(myID !!).collection("Follows")
            .orderBy("userID" , Query.Direction.DESCENDING).addSnapshotListener { snapshot , e ->
                if (e != null) {
                    Toast.makeText(requireActivity() , e.localizedMessage , Toast.LENGTH_LONG)
                        .show()

                } else {
                    if (snapshot != null) {
                        if (! snapshot.isEmpty) {
                            for (document in snapshot) {
                                val followsId = document.get("userID") as String?
                                if (userID == followsId) {
                                    isFollow = true
                                    binding.followButton.setText(R.string.unfollow)
                                    showComponent()
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun privateAccountData(userID : String) {
        db.collection("Users").document(userID).addSnapshotListener { snapshot , error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (snapshot != null) {
                    val accountStatus = snapshot.get("accountStatus") as Boolean?
                    if (accountStatus == true) {
                        hideComponent()
                    }
                }
            }
        }
    }

    private fun getRequestStatus(userID : String) {
        val myID = auth.currentUser?.uid.toString()
        db.collection("Users").document(userID).collection("FollowRequest")
            .addSnapshotListener { getData , e ->
                if (e != null) {
                    return@addSnapshotListener
                } else {
                    if (getData != null) {
                        if (! getData.isEmpty) {
                            val documents = getData.documents
                            for (document in documents) {
                                val followerID = document.get("userID") as String?
                                if (followerID == myID) {
                                    binding.privateAccountFollow.setText(R.string.request_completed)
                                } else {
                                    binding.privateAccountFollow.setText(R.string.title_followuser)
                                }
                            }
                        }

                    }
                }
            }

    }

    private fun installSettings(userID : String) {
        (requireActivity() as AppCompatActivity).setSupportActionBar(materialToolbar)
        val myActionBar : ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar
        myActionBar !!.setDisplayHomeAsUpEnabled(true)
        UserCache.getUser(userID) { userModel ->
            userModel?.let { user ->
                userName = user.userName
                profileImage = user.profileImage
                this.binding.upUserName.text = getString(R.string.user_name_tag , user.userName)

                when {
                    ! user.personalName.isNullOrBlank() -> {
                        binding.upPersonalName.visibility = View.VISIBLE
                        binding.upPersonalName.text = user.personalName
                    }

                    else -> {
                        binding.upPersonalName.visibility = View.GONE
                    }
                }

                when {
                    ! user.biography.isNullOrBlank() -> {
                        binding.upBiography.visibility = View.VISIBLE
                        binding.upBiography.text = user.biography
                    }

                    else -> {
                        binding.upBiography.visibility = View.GONE
                    }
                }
                when {
                    ! user.userLink.isNullOrBlank() -> {
                        binding.upLink.visibility = View.VISIBLE
                        binding.upLink.text = user.userLink
                    }

                    else -> {
                        binding.upLink.visibility = View.GONE
                    }
                }
                Glide.with(this@OtherUserFragment).load(user.profileImage)
                    .error(R.drawable.ic_rounded_user).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.myPhoto)
            } ?: run {
                UserCache.clear()
                return@getUser
            }
        }
        db.collection("Users").document(userID).collection("Follower")
            .addSnapshotListener { task , e ->
                if (e != null) {
                    return@addSnapshotListener
                } else {
                    if (task != null) {
                        val follower = task.size()
                        binding.followerCount.text =
                            getString(R.string.number_of_followers , follower.toString())
                    }
                }
            }
        db.collection("Users").document(userID).collection("Follows")
            .addSnapshotListener { task , e ->
                if (e != null) {
                    return@addSnapshotListener
                } else {
                    if (task != null) {
                        val follow = task.size().toString()
                        binding.followCount.text = getString(R.string.number_of_follows , follow)
                    }
                }
            }

        db.collection("Post").whereEqualTo("userID" , userID).addSnapshotListener { task , e ->
            if (e != null) {
                return@addSnapshotListener
            } else {
                if (task != null) {
                    val post = task.size().toString()
                    binding.postSize.text = getString(R.string.number_of_shares , post)
                }
            }
        }
    }

}