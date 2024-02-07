package com.emretaskesen.tpost.cache

import com.emretaskesen.tpost.model.Post
import com.google.firebase.firestore.FirebaseFirestore

object PostCache {
    //Verileri tutmak için MutableMap oluşturuyoruz
    private val cache = mutableMapOf<String, Post>()

    //Verileri çağırmak için fonksiyonumuzu oluşturuyoruz
    fun getPost(postID: String, callback: (Post?) -> Unit) {
        cache[postID]?.let {
            // Cache'den postu bulup callback ile dön
            callback(it)
        } ?: run {
            // Cache'de yoksa Firebase'den sorgula
            FirebaseFirestore.getInstance().collection("Post").document(postID)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val post = documentSnapshot.toObject(Post::class.java)
                    if (post != null) {
                        // Postu cache'e ekle
                        cache[postID] = post
                    }
                    // Callback ile postu dön
                    callback(post)
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