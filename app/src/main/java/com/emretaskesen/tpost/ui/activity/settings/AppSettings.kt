package com.emretaskesen.tpost.ui.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityAppSettingsBinding
import com.emretaskesen.tpost.ui.fragment.settings.SettingsFragment


class AppSettings : AppCompatActivity() {
    lateinit var binding: ActivityAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()

    }

    private fun initViewsAndFunctions(){
        loadFragment(SettingsFragment())
        setupToolbar()
    }

    private fun loadFragment(fragment : Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.settingContainer.id , fragment)
        transaction.commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.settingpageToolbar)
        val myActionBar: ActionBar? = supportActionBar
        myActionBar!!.setDisplayHomeAsUpEnabled(true)
        myActionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_left)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }

    }

}
