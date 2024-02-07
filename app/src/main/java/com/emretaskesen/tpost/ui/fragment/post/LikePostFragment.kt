package com.emretaskesen.tpost.ui.fragment.post

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.ui.adapter.PostAdapter
import com.emretaskesen.tpost.databinding.FragmentLikePostBinding
import com.emretaskesen.tpost.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LikePostFragment : Fragment() {
    private lateinit var binding : FragmentLikePostBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerViewAdapter : PostAdapter
    private var postList = arrayListOf<Post>()

    var userID : String? = null


    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentLikePostBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()
        userID = arguments?.getString("userID")
        getFirebaseData(userID!!)
    }

    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.likePostDetail.layoutManager = layoutManager
        recyclerViewAdapter = PostAdapter(requireActivity() , postList)
        binding.likePostDetail.adapter = recyclerViewAdapter
    }

    private fun getFirebaseData(currentUserID:String) {
        val likePost = mutableListOf<String>()
        val likeDocRef = db.collection("Users").document(currentUserID).collection("LikePost")
        likeDocRef.addSnapshotListener { data , e ->
            if (e != null) {
                Toast.makeText(requireActivity() , e.localizedMessage , Toast.LENGTH_LONG)
                    .show()
            }
            else {
                data?.let { snapshot ->
                    if (! snapshot.isEmpty) {
                        for (likeDoc in snapshot.documents) {
                            likeDoc.getString("postID")?.let { id ->
                                likePost.add(id)
                            }
                        }
                        // Takip edilen kişilerin her biri için gönderileri getir
                        getPostsFromFollows(likePost)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getPostsFromFollows(likePost : List<String>) {
        postList.clear()
        // Postaları almak için bir sorgu yap
        for (postId in likePost) {
            db.collection("Post").orderBy("postDate" , Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot , error ->
                    if (error != null) {
                        Toast.makeText(
                            requireActivity() ,
                            error.localizedMessage ,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else {
                        if (snapshot != null) {
                            if (! snapshot.isEmpty) {
                                val documents = snapshot.documents
                                for (document in documents) {
                                    val postID = document.get("postID") as String?
                                    val privatePost = document.get("privatePost") as Boolean?
                                    val archivePost = document.get("isArchive") as Boolean?
                                    val post = document.toObject(Post::class.java)
                                    if (postID == postId){
                                        if (archivePost==false||archivePost==null){
                                            if (privatePost == false || privatePost == null) {
                                                if (post!=null) postList.add(post)
                                            }
                                        }
                                    }
                                }
                                recyclerViewAdapter.notifyDataSetChanged()

                            }

                        }
                    }
                }
        }
    }
}