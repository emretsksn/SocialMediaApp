package com.emretaskesen.tpost.ui.activity.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityMainBinding
import com.emretaskesen.tpost.ui.activity.user.EditUserProfile
import com.emretaskesen.tpost.util.Controller
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    private var newMessage: Int = 0
    val userID = auth.currentUser!!.uid
    val userName = auth.currentUser!!.displayName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
    }

    override fun onResume() {
        super.onResume()
        checkForNewMessage()
        getMyData()
        initFirebase()
    }

    override fun onRestart() {
        super.onRestart()
        getMyData()
        initFirebase()
    }

    private fun initViewsAndFunctions() {
        // Navigation Controller'ı oluştur ve BottomNavigationView ile ilişkilendir
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainFragmentContainer) as NavHostFragment
        NavigationUI.setupWithNavController(binding.bottomNavigationView,
            navHostFragment.navController)

        // Gerekli kontrolleri ve Firebase işlemlerini başlat
        checkForNewMessage()
        getMyData()
        dataBaseUser()
        checkInternet()
        initFirebase()
    }

    private fun checkInternet() {
        if (!Controller().checkForInternet(this)) {
            // İnternet bağlantısı yoksa kullanıcıyı bilgilendir
            val alertDialog = AlertDialog.Builder(this, R.style.customAlert)
            alertDialog.setTitle(R.string.no_connection)
            alertDialog.setPositiveButton(R.string.title_activity_settings) { dialog, _ ->
                val settings = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                if (settings.resolveActivity(this.packageManager) != null) {
                    startActivity(settings)
                }
                dialog.dismiss()
            }
            // Diğer gerekirse işlemleri ekleyebilirsiniz
        }
    }

    private fun dataBaseUser() {
        // Kullanıcının bilgilerini Firestore'a kaydet
        val currentUser = auth.currentUser!!
        val usersMap = HashMap<String, Any>()
        usersMap["userID"] = currentUser.uid
        usersMap["userName"] = currentUser.displayName.toString()
        usersMap["userMail"] = currentUser.email.toString()
        usersMap["profileImage"] = currentUser.photoUrl.toString()

        db.collection("Users").document(currentUser.uid).update(usersMap)
            .addOnCompleteListener { }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun checkForNewMessage() {
        // Yeni mesajları kontrol et ve BottomNavigationView'daki badge'i güncelle
        val messageItem = binding.bottomNavigationView.menu.getItem(3).itemId
        val badge = binding.bottomNavigationView.getOrCreateBadge(messageItem)
        badge.backgroundColor = getColor(R.color.green)
        db.collection("Users").document(userID).collection("MyMessages")
            .whereEqualTo("messageCount", true).addSnapshotListener { value, error ->
                if (error != null) {
                    // Hata durumunu handle et
                    return@addSnapshotListener
                }
                newMessage = value?.size() ?: 0

                if (newMessage > 0) {
                    // Yeni mesaj var, badge'i göster
                    badge.number = newMessage
                    badge.isVisible = true
                } else {
                    // Yeni mesaj yok, badge'i gizle
                    badge.isVisible = false
                }
            }
    }

    private fun initFirebase() {
        // Firebase servislerini başlat
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseAnalytics.getInstance(this).logEvent("main_page", null)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                val tokenMap = hashMapOf<String, Any>()
                tokenMap["fcmToken"] = fcmToken
                db.collection("Users").document(userID).update(tokenMap)
            }
        }
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val deviceID = task.result

                val tokenMap = hashMapOf<String, Any>()
                tokenMap["deviceID"] = deviceID
                db.collection("Users").document(userID).update(tokenMap)
            }
        }

        db.collection("UserName").document(userID).get().addOnSuccessListener { snapshot ->
            val userNames = snapshot.get("userName") as String?
            if (userNames == userName) {
                return@addOnSuccessListener
            } else {
                val userNameMap = HashMap<String, Any>()
                userNameMap["userName"] = userName!!
                userNameMap["userID"] = userID
                db.collection("UserName").document(userID).set(userNameMap)
            }
        }
    }

    private fun getMyData() {
        // Kullanıcı verilerini kontrol et ve gerekirse EditUserProfile aktivitesine yönlendir
        db.collection("Users").document(userID).get().addOnSuccessListener { task ->
            val accountData = task.get("accountStatus") as Boolean?
            val isNew = task.get("isNew") as Boolean?
            if (isNew == true) {
                val intent = Intent(this@MainActivity, EditUserProfile::class.java).apply {
                    putExtra("getType", 1)
                }
                startActivity(intent)
            }
            if (accountData == true) {
                postsHide()
            } else {
                postsShow()
            }
        }
    }

    private fun postsHide() {
        // Kullanıcının postlarını gizle
        db.collection("Post").whereEqualTo("userID", userID).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                // Batch işlemi başlat
                val batch = db.batch()

                // Her belge için `privatePost`'u güncelle
                for (document in documents) {
                    val docRef = db.collection("Post").document(document.id)
                    batch.update(docRef, "privatePost", true)
                }

                // Batch işlemini uygula
                batch.commit().addOnCompleteListener {
                    if (it.isSuccessful) {
                        return@addOnCompleteListener
                    } else {
                        return@addOnCompleteListener
                    }
                }
            }
    }

    private fun postsShow() {
        // Kullanıcının postlarını göster
        db.collection("Post").whereEqualTo("userID", userID).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                // Batch işlemi başlat
                val batch = db.batch()

                // Her belge için `privatePost`'u güncelle
                for (document in documents) {
                    val docRef = db.collection("Post").document(document.id)
                    batch.update(docRef, "privatePost", false)
                }

                // Batch işlemini uygula
                batch.commit().addOnCompleteListener {
                    if (it.isSuccessful) {
                        return@addOnCompleteListener
                    } else {
                        return@addOnCompleteListener
                    }
                }
            }
    }
}
