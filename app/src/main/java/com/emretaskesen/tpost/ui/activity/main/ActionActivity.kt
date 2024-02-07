package com.emretaskesen.tpost.ui.activity.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityActionBinding
import com.emretaskesen.tpost.ui.fragment.images.GalleryFragment
import com.emretaskesen.tpost.ui.fragment.user.UsersFragment
import com.google.firebase.auth.FirebaseAuth

class ActionActivity : AppCompatActivity() {
    lateinit var binding: ActivityActionBinding
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        when (intent.getIntExtra("getType" , 1)) {
            1 -> {
                loadFragment(UsersFragment())
            }

            2 -> {
                loadFragment(GalleryFragment())
            }
        }
    }


    private fun loadFragment(fragment : Fragment) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val bundle = Bundle()
        bundle.putString("userID" , userID)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.actionContainerMain , fragment)
        transaction.commit()
    }
}