package com.emretaskesen.tpost.ui.activity.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivitySplashScreenBinding
import com.emretaskesen.tpost.ui.activity.user.Auth


@SuppressLint("CustomSplashScreen")
class SplashScreen: AppCompatActivity() {
    lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {
        @Suppress("DEPRECATION") window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Auth::class.java)
            startActivity(intent)
            this.finish()
        }, 1000) // 3000 is the delayed time in milliseconds.
    }

}