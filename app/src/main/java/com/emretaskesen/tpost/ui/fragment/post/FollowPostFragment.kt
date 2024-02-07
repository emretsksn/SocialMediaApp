package com.emretaskesen.tpost.ui.fragment.post

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.PostAdapter
import com.emretaskesen.tpost.databinding.FragmentFollowPostBinding
import com.emretaskesen.tpost.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber

class FollowPostFragment : Fragment() {

    private lateinit var binding : FragmentFollowPostBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerViewAdapter : PostAdapter
    private var postList = arrayListOf<Post>()

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentFollowPostBinding.inflate(inflater , container , false)
        return binding.root

    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()
        getFirebaseData()
    }

    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.discorverPostDetail.layoutManager = layoutManager
        recyclerViewAdapter = PostAdapter(requireActivity() , postList)
        binding.discorverPostDetail.adapter = recyclerViewAdapter
    }

    private fun getFirebaseData() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID != null) {
            // Takip edilen kullanıcı ID'leri depolamak için bir liste oluştur
            val followsIdList = mutableListOf<String>()
            followsIdList.add(currentUserID)
            // Takip edilen kişileri getir
            val followsRef = db.collection("Users").document(currentUserID).collection("Follows")
            followsRef.addSnapshotListener { data , e ->
                if (e != null) {
                    Toast.makeText(requireActivity().applicationContext , e.localizedMessage , Toast.LENGTH_LONG)
                        .show()
                }
                else {
                    data?.let { snapshot ->
                        if (! snapshot.isEmpty) {
                            for (folDoc in snapshot.documents) {
                                folDoc.getString("userID")?.let { id ->
                                    followsIdList.add(id)
                                }
                            }
                            // Takip edilen kişilerin her biri için gönderileri getir
                            getPostsFromFollows(followsIdList)
                            emptyShares(false,2)
                        }else{
                            emptyShares(true,2)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getPostsFromFollows(followsIdList : List<String>) {
        // Postaları almak için bir sorgu yap
        for (followsId in followsIdList) {
            db.collection("Post").orderBy("postDate" , Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot , error ->
                    if (error != null) {
                        Timber.tag("Error").e(error.localizedMessage)
                    }
                    else {
                        if (snapshot != null) {
                            postList.clear()
                            if (! snapshot.isEmpty) {
                                val documents = snapshot.documents
                                for (document in documents) {
                                    val userID = document.get("userID") as String?
                                    val archivePost = document.get("isArchive") as Boolean?
                                    val post = document.toObject(Post::class.java)
                                    if (archivePost==false||archivePost==null){
                                        if (userID == followsId) {
                                            if (post!=null) postList.add(post)
                                            emptyShares(false,1)
                                        }
                                    }else{
                                        emptyShares(true,1)
                                    }
                                }
                                recyclerViewAdapter.notifyDataSetChanged()
                                emptyShares(false,1)
                            }else{
                                emptyShares(true,1)
                            }

                        }else{
                            emptyShares(true,1)
                        }
                    }
                }
        }
    }

    private fun emptyShares(isEmpty:Boolean,type:Int){
        val bannerText = binding.emptyText
        when {
            isEmpty -> {
                if (type == 1) bannerText.setText(R.string.null_post)
                else if (type == 2) bannerText.setText(R.string.null_follow)
                bannerText.visibility = View.VISIBLE
            }
            else -> {
                bannerText.visibility = View.GONE
            }
        }
    }

}
