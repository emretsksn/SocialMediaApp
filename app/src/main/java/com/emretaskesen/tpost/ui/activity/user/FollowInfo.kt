package com.emretaskesen.tpost.ui.activity.user

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.HomeFragmentStateAdapter
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.ActivityFollowInfoBinding
import com.emretaskesen.tpost.ui.fragment.user.FollowerFragment
import com.emretaskesen.tpost.ui.fragment.user.FollowingFragment
import com.google.android.material.tabs.TabLayoutMediator

class FollowInfo : AppCompatActivity() {
    private var _binding : ActivityFollowInfoBinding? = null
    private val binding get() = _binding !!
    private lateinit var adapter : HomeFragmentStateAdapter
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFollowInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {
        val userID = intent.getStringExtra("userID") ?: throw IllegalStateException("UserID is required")
        val followingFragment = FollowingFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        }

        val followerFragment = FollowerFragment().also {
            it.arguments = Bundle().apply {
                putString("userID" , userID)
            }
        }

        // Fragment listesini yarat ve fragmentları ekle
        val fragmentList = arrayListOf(followingFragment , followerFragment)
        initViewPagerAdapter(fragmentList)

        setSupportActionBar(binding.followInfoToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initMenu()
        UserCache.getUser(userID){ userModel ->
            userModel?.let {
                supportActionBar?.title = userModel.userName
            }
        }
    }

    private fun initMenu() {
        val menuHost : MenuHost = this
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        finish()
                        true
                    }
                    else -> false
                }
            }
        })
    }

    private fun initViewPagerAdapter(fragmentList : ArrayList<Fragment>) { // Adapter init
        val viewPager = binding.vpHome
        adapter = HomeFragmentStateAdapter(supportFragmentManager ,
            lifecycle ,
            fragmentList)
        viewPager.adapter = adapter

        // TabLayout ile ViewPager bağlantısı yapılır.
        TabLayoutMediator(binding.followTab , viewPager) { tab , position ->
            when (position) {
                0 -> {
                    tab.setText(getString(R.string.follower_text))
                }

                1 -> {
                    tab.setText(getString(R.string.following_text))
                }
            }
        }.attach()
    }
}