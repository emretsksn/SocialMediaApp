package com.emretaskesen.tpost.cache

import com.emretaskesen.tpost.model.UserModel
import com.google.firebase.firestore.FirebaseFirestore

object UserCache {
    //Verileri tutmak için MutableMap oluşturuyoruz
    private val cache = mutableMapOf<String, UserModel>()

    //Verileri çağırmak için fonksiyonumuzu oluşturuyoruz
    fun getUser(userID: String, callback: (UserModel?) -> Unit) {
        cache[userID]?.let {
            // Cache'den kullanıcıyı bulup callback ile dön
            callback(it)
        } ?: run {
            FirebaseFirestore.getInstance().collection("Users").document(userID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(UserModel::class.java)
                    if (user != null) {
                        // Kullanıcıyı cache'e ekle
                        cache[userID] = user
                    }
                    // Callback ile kullanıcıyı dön dön
                    callback(user)
                }
                .addOnFailureListener {
                    // Hata durumunda callback ile null dön
                    callback(null)
                }
        }
    }

    fun clear() {
        // Önbelleği temizle
        cache.clear()
    }

}