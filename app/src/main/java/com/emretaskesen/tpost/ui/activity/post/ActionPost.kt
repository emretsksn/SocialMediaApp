package com.emretaskesen.tpost.ui.activity.post

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityActionPostBinding
import com.emretaskesen.tpost.ui.fragment.post.ArchivedPostFragment
import com.emretaskesen.tpost.ui.fragment.post.SavedPostFragment
import com.emretaskesen.tpost.ui.fragment.post.LikePostFragment
import com.google.firebase.auth.FirebaseAuth

class ActionPost : AppCompatActivity() {
    lateinit var binding : ActivityActionPostBinding
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActionPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when (intent.getIntExtra("getType" , 1)) {
            1 -> {
                savedSetup()
            }

            2 -> {
                likedSetup()
            }

            3 -> {
                archiveSetup()
            }

            else -> {

            }
        }

    }


    private fun archiveSetup() {
        setSupportActionBar(binding.actionPostToolbar)
        val myActionBar = supportActionBar
        myActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = getString(R.string.archive_post)
        }
        initMenu()
        loadFragment(ArchivedPostFragment())
    }

    private fun savedSetup() {
        setSupportActionBar(binding.actionPostToolbar)
        val myActionBar = supportActionBar
        myActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = getString(R.string.saved_post)
        }
        initMenu()
        loadFragment(SavedPostFragment())
    }

    private fun likedSetup() {
        setSupportActionBar(binding.actionPostToolbar)
        val myActionBar = supportActionBar
        myActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = getString(R.string.liked_post)
        }
        initMenu()
        loadFragment(LikePostFragment())
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

    private fun loadFragment(fragment : Fragment) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val bundle = Bundle()
        bundle.putString("userID" , userID)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.actionPostContainer , fragment)
        transaction.commit()
    }
}