package com.emretaskesen.tpost.ui.activity.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityUserprofileBinding
import com.emretaskesen.tpost.ui.fragment.user.OtherUserFragment
import com.emretaskesen.tpost.ui.fragment.user.UserProfileFragment

import com.google.firebase.auth.FirebaseAuth


class UserProfile : AppCompatActivity() {
    lateinit var binding: ActivityUserprofileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val userID = intent.getStringExtra("userID")
        val myID = FirebaseAuth.getInstance().currentUser?.uid
        if (userID==myID){
            loadFragment(UserProfileFragment() ,myID!!,true)
        }else{
            loadFragment(OtherUserFragment() ,userID!!,false)
        }
    }
    private fun loadFragment(fragment: Fragment, userID: String,isYour: Boolean) {
        val bundle = Bundle()
        bundle.putString("userID", userID)
        bundle.putBoolean("isYour",isYour)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.userProfileFragmentContainer, fragment)
        transaction.commit()
    }
}

