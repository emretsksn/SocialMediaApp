package com.emretaskesen.tpost.ui.fragment.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.FragmentHomePageBinding
import com.emretaskesen.tpost.ui.activity.user.EditUserProfile
import com.emretaskesen.tpost.ui.activity.user.Auth
import com.emretaskesen.tpost.ui.activity.main.ActionActivity
import com.emretaskesen.tpost.ui.activity.post.ActionPost
import com.emretaskesen.tpost.ui.activity.settings.AppInfo
import com.emretaskesen.tpost.ui.activity.settings.AppSettings
import com.emretaskesen.tpost.ui.activity.user.NotificationPage
import com.emretaskesen.tpost.ui.fragment.post.FollowPostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import timber.log.Timber
import java.lang.Exception


class HomePageFragment : Fragment() , NavigationView.OnNavigationItemSelectedListener {
    // View'lara erişebilmek için binding nesnesi
    private var _binding : FragmentHomePageBinding? = null
    var auth = FirebaseAuth.getInstance()
    val db = com.google.firebase.Firebase.firestore
    private val binding get() = _binding !!
    val profileImage = FirebaseAuth.getInstance().currentUser?.photoUrl
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
    private lateinit var materialToolbar : MaterialToolbar
    private lateinit var drawerLayout : DrawerLayout
    private lateinit var navView : NavigationView
    private lateinit var toggle : ActionBarDrawerToggle
    private var userID : String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        _binding = FragmentHomePageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        userID = auth.currentUser?.uid
        initMenu()
        installSettings()
        dataCurrent()
        joinTimeListener()
        checkNotify()
    }

    private fun initMenu() {

        val menuHost : MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_mainpage , menu)
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home ->{
                        true
                    }
                    R.id.notificationFragment -> {
                        startActivity(Intent(requireActivity(), NotificationPage::class.java))
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        checkNotify()
    }

    private fun joinTimeListener() {
        val userID = auth.currentUser?.uid
        val userDocument = db.collection("Users").document(userID !!)
        val updateMap = hashMapOf<String , Any>()
        val joinTime = Timestamp.now()
        updateMap["joinTime"] = joinTime
        userDocument.update(updateMap)
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun checkNotify() {
        val myID = auth.currentUser?.uid ?: return
        // Toolbar menüdeki belirli bir öğeyi bağla
        val menuItemId = R.id.notificationFragment // Menü öğesi ID'nizi buraya girin
        val badge = BadgeDrawable.create(requireContext())
        val toolbar = materialToolbar
        // Gerçek zamanlı güncellemeler için Firestore dinleyicisi
        db.collection("Users").document(myID).collection("Notification").whereEqualTo("isNew",true)
            .addSnapshotListener { snapshot , e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val newNotifCount = snapshot.size()
                if (newNotifCount>0){
                    badge.number = newNotifCount
                    badge.isVisible = true
                }else{
                    badge.isVisible = false
                }
                BadgeUtils.attachBadgeDrawable(badge , toolbar , menuItemId)

            }
    }

    @SuppressLint("SetTextI18n")
    private fun dataCurrent() {
        val navigationView : NavigationView? = view?.findViewById(R.id.nav_view)
        val headerView = navigationView?.getHeaderView(0)
        val userName = headerView?.findViewById<MaterialTextView>(R.id.nh_user_name)
        val personalName = headerView?.findViewById<MaterialTextView>(R.id.nh_personal_name)
        val followerCount = headerView?.findViewById<MaterialTextView>(R.id.nh_user_follower)
        val followCount = headerView?.findViewById<MaterialTextView>(R.id.nh_user_following)
        val userProfile = headerView?.findViewById<CircleImageView>(R.id.nh_user_profil)
        val profileCard = headerView?.findViewById<CardView>(R.id.profileCard)
        val currentUserID = auth.currentUser!!.uid
        UserCache.getUser(currentUserID) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                personalName?.text =
                        user.personalName ?: getString(R.string.anonymous_user)
                userName?.text = getString(R.string.user_name_tag , user.userName)
                Glide.with(this@HomePageFragment)
                    .load(user.profileImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(userProfile!!)
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
                        if (followerCount != null) {
                            followerCount.text = getString(R.string.number_of_followers , follower.toString())
                        }
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
                        if (followCount != null) {
                            followCount.text = getString(R.string.number_of_follows , follow)
                        }
                    }
                }
            }
        profileCard?.setOnClickListener {
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.selectedItemId=R.id.userProfileFragment
        }
    }

    private fun installSettings() {
        loadFragment(FollowPostFragment())
        materialToolbar = view?.findViewById(R.id.mainpageToolbar) !!
        (requireActivity() as AppCompatActivity).setSupportActionBar(materialToolbar)
        /*val myActionBar : androidx.appcompat.app.ActionBar? =
                (requireActivity() as AppCompatActivity).supportActionBar*/

        // Navigation Drawer işlemleri
        drawerLayout = view?.findViewById(R.id.drawer_layout) !!
        navView = view?.findViewById(R.id.nav_view) !!

        toggle = ActionBarDrawerToggle(
            activity , drawerLayout , materialToolbar , R.string.nav_open , R.string.nav_close
        ).apply {
            setHomeAsUpIndicator(R.drawable.ic_subject)
            drawerLayout.addDrawerListener(this)
            syncState()
        }
        toggle.setHomeAsUpIndicator(R.drawable.ic_subject)
        navView.setNavigationItemSelectedListener(this)


        requireActivity().onBackPressedDispatcher.addCallback(requireActivity() /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)){
                    drawerLayout.closeDrawer(GravityCompat.START)
                }else{
                    requireActivity().moveTaskToBack(true)
                }
            }
        })
    }



    private fun loadFragment(fragment : Fragment) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val bundle = Bundle()
        bundle.putString("userID" , userID)
        fragment.arguments = bundle
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.mainPageContainer , fragment)
        transaction.commit()
    }


    override fun onNavigationItemSelected(item : MenuItem) : Boolean {
        when (item.itemId) {

            R.id.usersFragment -> {
                val intent = Intent(requireActivity(), ActionActivity::class.java).apply {
                    putExtra("getType",1)
                }
                requireActivity().startActivity(intent)
                
            }

            R.id.editProfile -> {
                val intent = Intent(requireActivity(), EditUserProfile::class.java).apply {
                    putExtra("getType",1)
                }
                requireActivity().startActivity(intent)
                
            }

            R.id.savedPost -> {
                val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                    putExtra("getType",1)
                }
                requireActivity().startActivity(intent)
                
            }
            R.id.likedPost -> {
                val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                    putExtra("getType",2)
                }
                requireActivity().startActivity(intent)
                
            }
            R.id.archivedPost -> {
                val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                    putExtra("getType",3)
                }
                requireActivity().startActivity(intent)
                
            }

            R.id.nav_logout -> {
                logOut()
            }

            R.id.nav_appsetting -> {
                startActivity(Intent(requireActivity(), AppSettings::class.java))
                
            }

            R.id.nav_appinfo -> {
                startActivity(Intent(activity , AppInfo::class.java))
                
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logOut() {
        val signOutAlert = androidx.appcompat.app.AlertDialog.Builder(requireActivity())
        signOutAlert.setTitle(R.string.sign_out_alert_title)
        signOutAlert.setMessage(
            R.string.sign_out_alert_message)
        signOutAlert.setIcon(R.drawable.ic_log_out)
        signOutAlert.setPositiveButton(
            R.string.approve
        ) { _ , _ ->
            try {
                FirebaseAuth.getInstance().signOut()
            }catch (e:Exception){
                e.localizedMessage?.let { Timber.tag("LogOutFailed").e(it) }
            }finally {
                startActivity(Intent(requireActivity(), Auth::class.java))
                requireActivity().finish()
            }

        }

        signOutAlert.setNeutralButton(
            R.string.cancel
        ) { _ , _ ->
            Toast.makeText(
                requireContext() , R.string.sign_out_toast_text , Toast.LENGTH_LONG
            ).show()
        }

        signOutAlert.show()
    }
}
