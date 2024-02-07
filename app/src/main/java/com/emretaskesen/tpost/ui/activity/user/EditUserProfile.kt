package com.emretaskesen.tpost.ui.activity.user

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityEditUserProfileBinding
import com.emretaskesen.tpost.ui.fragment.user.AccountSettingFragment
import com.emretaskesen.tpost.ui.fragment.user.EditAccountFragment
import com.google.firebase.auth.FirebaseAuth

class EditUserProfile : AppCompatActivity() {
    lateinit var binding: ActivityEditUserProfileBinding
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        when (intent.getIntExtra("getType" , 1)) {
            1 -> {
                loadFragment(EditAccountFragment())
            }

            2 -> {
                loadFragment(AccountSettingFragment())
            }
        }
    }

    private fun loadFragment(fragment : Fragment) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val bundle = Bundle()
        bundle.putString("userID" , userID)
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.editUserContainer , fragment)
        transaction.commit()
    }
}