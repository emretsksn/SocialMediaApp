package com.emretaskesen.tpost.ui.activity.settings

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityAppinfoBinding
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ktx.firestore

class AppInfo : AppCompatActivity() {
    val db = com.google.firebase.ktx.Firebase.firestore
    lateinit var auth: FirebaseAuth
    lateinit var binding: ActivityAppinfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppinfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        binding.sendBugButton.setOnClickListener {
            val version = binding.appVersion.text.toString()
            val bugLink = binding.bugLinkText.text.toString()
            val bugDetail = binding.bugDetailText.text.toString()
            val userID = auth.currentUser?.uid
            val userName = auth.currentUser?.displayName
            val reportTime = Timestamp.now()
            val bugMap = HashMap<String,Any>()
            bugMap["userID"] = userID!!
            bugMap["userName"] = userName!!
            bugMap["version"] = version
            bugMap["bugDetail"] = bugDetail
            bugMap["bugLink"] = bugLink
            bugMap["reportTime"] = reportTime
            db.collection("Bugs").document().set(bugMap).addOnSuccessListener {
                binding.bugDetailText.text = null
                binding.bugLinkText.text = null
                Toast.makeText(applicationContext,"Hata bildirildi.",Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(applicationContext,"Hata bildiriminde sorun oluştu. Tekrar deneyiniz.",Toast.LENGTH_LONG).show()
            }

        }

        setupToolbar()
    }

    private fun setupToolbar() {
        binding.settingpageToolbar.setNavigationIcon(R.drawable.ic_arrow_left)
        binding.settingpageToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    fun goWebSite(view: View) {
        val url = getString(R.string.website_url)
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
        view.animation
    }


}