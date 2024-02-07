package com.emretaskesen.tpost.ui.fragment.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.emretaskesen.tpost.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SettingsFragment : PreferenceFragmentCompat() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreatePreferences(savedInstanceState : Bundle? , rootKey : String?) {
        setPreferencesFromResource(R.xml.setting_preference , rootKey)
        setupChangeListeners()
        getMyData()
    }

    private fun setupChangeListeners() {
        themePreference()
        notifyPreference()
        accountPrivacy()
        activityStatus()
        getTheme()
    }

    private fun themePreference(){
        // Tercihinize direkt referans alıp dinleyici ekleyin
        val themePreference : ListPreference? = findPreference(getString(R.string.key_app_theme))
        themePreference?.let {
            themePreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _ , newValue -> // Yeni değer seçildi, burada işleyin
                    // Burada seçilen tema değerine göre iş yapabilirsiniz
                    when (newValue as String) {
                        "0" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            themePreference.summary = getString(R.string.defaultSystemSetting)
                        }

                        "1" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            themePreference.summary = getString(R.string.light)
                        }

                        "2" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            themePreference.summary = getString(R.string.dark)
                        }

                    }
                    true // Güncelleme işlemini kabul ediyoruz
                }
        }

    }

    private fun getTheme(){
        val themePreference : ListPreference? = findPreference(getString(R.string.key_app_theme))
        themePreference?.let {
            when(themePreference.value){
                "0" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    themePreference.summary = getString(R.string.defaultSystemSetting)
                }

                "1" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    themePreference.summary = getString(R.string.light)
                }

                "2" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    themePreference.summary = getString(R.string.dark)
                }
            }
        }

    }

    private fun notifyPreference(){
        val notificationPreference : SwitchPreference? =
            findPreference(getString(R.string.key_app_notification))
        notificationPreference?.let {
            notificationPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _ , newValue ->
                    when (newValue as Boolean) {
                        true -> {
                            val toast = Toast(requireActivity())
                            toast.apply {
                                duration = Toast.LENGTH_LONG
                                setText(getString(R.string.notification_on))
                                setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
                            }
                            toast.show()
                        }
                        false -> {
                            val toast = Toast(requireActivity())
                            toast.apply {
                                duration = Toast.LENGTH_LONG
                                setText(getString(R.string.notification_off))
                                setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
                            }
                            toast.show()
                        }
                    }
                    true
                }
        }
    }
    private fun accountPrivacy(){
        val accountPrivacyPreference : SwitchPreference? =
            findPreference(getString(R.string.key_account_privacy))
        accountPrivacyPreference?.let {
            accountPrivacyPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _ , newValue ->
                    updateOnlineStatus(newValue as Boolean)
                    true
                }
        }
    }

    private fun activityStatus(){
        val activityStatusPreference : SwitchPreference? =
            findPreference(getString(R.string.key_account_activity))
        activityStatusPreference?.let {
            activityStatusPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _ , newValue ->
                    updateAccountStatus(newValue as Boolean)
                    true
                }
        }
    }


    private fun updateOnlineStatus(onlineStatus: Boolean){
        val myId = auth.currentUser?.uid
        val userMap = HashMap<String,Any>()
        userMap["onlineStatus"] = onlineStatus
        db.collection("Users").document(myId!!).update(userMap).addOnSuccessListener {

        }
    }
    private fun updateAccountStatus(accountStatus:Boolean){
        val myId = auth.currentUser?.uid
        val userMap = HashMap<String,Any>()
        userMap["accountStatus"] = accountStatus
        db.collection("Users").document(myId!!).update(userMap).addOnSuccessListener {

        }
    }

    private fun getMyData(){
        val activityStatusPreference : SwitchPreference? =
            findPreference(getString(R.string.key_account_activity))
        val accountPrivacyPreference : SwitchPreference? =
            findPreference(getString(R.string.key_account_privacy))
        val currentUserID = auth.currentUser!!.uid
        db.collection("Users").document(currentUserID).get().addOnSuccessListener { task->
            activityStatusPreference?.setDefaultValue(task.getBoolean("onlineStatus"))
            accountPrivacyPreference?.setDefaultValue(task.getBoolean("accountStatus"))
        }
    }
}