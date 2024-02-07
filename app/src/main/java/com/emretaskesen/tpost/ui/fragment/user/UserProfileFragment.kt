package com.emretaskesen.tpost.ui.fragment.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
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
import com.emretaskesen.tpost.databinding.FragmentUserprofileBinding
import com.emretaskesen.tpost.ui.activity.user.EditUserProfile
import com.emretaskesen.tpost.ui.activity.user.FollowInfo
import com.emretaskesen.tpost.ui.fragment.post.MentionPostFragment
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileFragment : Fragment(){

    // View'lara erişebilmek için binding nesnesi
    private lateinit var binding : FragmentUserprofileBinding
    private var auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fragmentManager : FragmentManager
    private lateinit var adapter : HomeFragmentStateAdapter

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        // ui bağlantısı
        binding = FragmentUserprofileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        val userPostFragment = UserPostFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        }

        val mentionPostFragment = MentionPostFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        }

        // Fragment listesini yarat ve fragmentları ekle
        val fragmentList = arrayListOf(userPostFragment , mentionPostFragment)
        fragmentManager = requireActivity().supportFragmentManager
        // Adapteri yarat ve viewpager'a set et
        initViewPagerAdapter(fragmentList)
        initMenu()
        installSettings()
        initViewsAndFunctions()
    }

    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                // Add menu items here
               /* menuInflater.inflate(R.menu.menu_userprofile , menu)*/
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home ->{
                        requireActivity().finish()
                        true
                    }
                    /*R.id.settingAccount -> {
                        val accountSetting = AccountOptionFragment()
                        val tag = AccountOptionFragment::class.java.simpleName
                        accountSetting.show(requireActivity().supportFragmentManager, tag)
                        true

                    }*/
                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }

    private fun initViewPagerAdapter(fragmentList : ArrayList<Fragment>) {
        // Adapter init
        val viewPager = binding.vpHome
        adapter = HomeFragmentStateAdapter(
            childFragmentManager , viewLifecycleOwner.lifecycle , fragmentList
        )
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

    private fun initViewsAndFunctions() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val editProfileButton = binding.editProfileButton
        editProfileButton.setOnClickListener {
            val intent = Intent(requireActivity(), EditUserProfile::class.java).apply {
                putExtra("getType",1)
            }
            requireActivity().startActivity(intent)
        }
        val myPhoto = auth.currentUser?.photoUrl
        binding.myPhoto.setOnLongClickListener {
            val showImageFragment =
                ShowImageFragment.newInstance(myPhoto)
            // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
            showImageFragment.show(
                requireActivity().supportFragmentManager,
                showImageFragment.tag
            )
            true
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

    }


    private fun installSettings() {
        val currentUserID = auth.currentUser?.uid
        val isYour = arguments?.getBoolean("isYour")
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.userprofileToolbar)
        val myActionBar : ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar
        if (isYour==true){
            myActionBar?.run {
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
                setHomeAsUpIndicator(R.drawable.ic_arrow_left)
            }
        }else{
            myActionBar?.run {
                setDisplayHomeAsUpEnabled(false)
                setHomeButtonEnabled(false)
            }
        }
        UserCache.getUser(currentUserID !!) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                binding.upUserName.text = getString(R.string.user_name_tag , user.userName)
                when {
                    !user.personalName.isNullOrBlank() -> {
                        binding.upPersonalName.visibility = View.VISIBLE
                        binding.upPersonalName.text =
                            user.personalName
                    }
                    else -> {
                        binding.upPersonalName.visibility = View.GONE
                    }
                }

                when {
                    !user.biography.isNullOrBlank() -> {
                        binding.upBiography.visibility = View.VISIBLE
                        binding.upBiography.text = user.biography
                    }
                    else -> {
                        binding.upBiography.visibility = View.GONE
                    }
                }
                when {
                    !user.userLink.isNullOrBlank() -> {
                        binding.upLink.visibility = View.VISIBLE
                        binding.upLink.text = user.userLink
                    }
                    else -> {
                        binding.upLink.visibility = View.GONE
                    }
                }
                Glide.with(this@UserProfileFragment)
                    .load(user.profileImage)
                    .error(R.drawable.ic_rounded_user)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.myPhoto)
            } ?: run {
                UserCache.clear()
                return@getUser
            }
        }

        db.collection("Users").document(currentUserID).collection("Follower")
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
        db.collection("Users").document(currentUserID).collection("Follows")
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
        db.collection("Post").whereEqualTo("userID" , currentUserID).addSnapshotListener { task , e ->
            if (e != null) {
                return@addSnapshotListener
            }
            else {
                if (task != null){
                    val post = task.size().toString()
                    binding.postSize.text = getString(R.string.number_of_shares , post)
                }
            }
        }
    }


}

