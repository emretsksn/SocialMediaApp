package com.emretaskesen.tpost.ui.activity.user

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityAuthBinding
import com.emretaskesen.tpost.ui.activity.main.MainActivity
import com.emretaskesen.tpost.ui.fragment.user.LoginFragment
import com.emretaskesen.tpost.util.ConstVal.Permissions.REQUEST_CODE
import com.google.firebase.auth.FirebaseAuth

class Auth : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth
    lateinit var binding : ActivityAuthBinding


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val permissions = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES ,
        Manifest.permission.READ_MEDIA_AUDIO ,
        Manifest.permission.READ_MEDIA_VIDEO ,
        Manifest.permission.CAMERA ,
        Manifest.permission.WRITE_EXTERNAL_STORAGE ,
        Manifest.permission.READ_EXTERNAL_STORAGE ,
        Manifest.permission.INTERNET ,
        Manifest.permission.ACCESS_NETWORK_STATE ,
        Manifest.permission.WAKE_LOCK ,
        Manifest.permission.VIBRATE ,
        Manifest.permission.RECEIVE_BOOT_COMPLETED ,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
        loadFragment(LoginFragment())
    }

    private fun loadFragment(fragment : Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView , fragment)
        transaction.commit()
    }

    @SuppressLint("NewApi")
    private fun initViewsAndFunctions() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this , MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        checkAndRequestPermissions()
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean = when (item.itemId) {
        android.R.id.home -> {
            supportFragmentManager.popBackStackImmediate()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val listPermissionsNeeded = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this , permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this , listPermissionsNeeded.toTypedArray() , REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode : Int ,
        permissions : Array<out String> ,
        grantResults : IntArray ,
    ) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return
                }
                return
            }

            else -> {

            }
        }
    }
}