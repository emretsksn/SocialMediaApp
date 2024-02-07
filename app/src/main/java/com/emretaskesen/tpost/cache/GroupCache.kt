package com.emretaskesen.tpost.cache

import com.emretaskesen.tpost.model.Group
import com.google.firebase.firestore.FirebaseFirestore

object GroupCache {
    //Verileri tutmak için MutableMap oluşturuyoruz
    private val cache = mutableMapOf<String, Group>()

    //Verileri çağırmak için fonksiyonumuzu oluşturuyoruz
    fun getGroup(groupID: String, callback: (Group?) -> Unit) {
        cache[groupID]?.let {
            // Cache'den grubu bulup callback ile dön
            callback(it)
        } ?: run {
            // Cache'de yoksa Firebase'den sorgula
            FirebaseFirestore.getInstance().collection("Groups").document(groupID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val messages = documentSnapshot.toObject(Group::class.java)
                    if (messages != null) {
                        // Grubu cache'e ekle
                        cache[groupID] = messages
                    }
                    // Callback ile Grubu dön
                    callback(messages)
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