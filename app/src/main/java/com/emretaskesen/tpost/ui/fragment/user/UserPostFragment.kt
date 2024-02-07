package com.emretaskesen.tpost.ui.fragment.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.ui.adapter.PostAdapter
import com.emretaskesen.tpost.databinding.FragmentUserPostBinding
import com.emretaskesen.tpost.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber

class UserPostFragment : Fragment() {

    private lateinit var binding : FragmentUserPostBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerViewAdapter : PostAdapter
    private var postList = arrayListOf<Post>()
    var userID : String? = null

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentUserPostBinding.inflate(inflater , container , false)
        return binding.root

    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()
        val userID = arguments?.getString("userID")

        if (userID != null) {
            initViewsAndFunctions()
            getMyPost(userID)
        }
        else {
            requireActivity().finish()
            Toast.makeText(requireActivity(), "Kullanıcı bilgisi alınamadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.userPostDetail.layoutManager = layoutManager
        recyclerViewAdapter = PostAdapter(requireActivity() , postList)
        binding.userPostDetail.adapter = recyclerViewAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getMyPost(userID : String) {
        db.collection("Post").whereEqualTo("userID" , userID)
            .orderBy("postDate" , Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot , error ->
                if (error != null) {
                    Toast.makeText(requireActivity(), error.localizedMessage, Toast.LENGTH_LONG).show()
                    error.localizedMessage?.let { Timber.tag("User").e(it) }
                }
                else {
                    if (snapshot != null) {
                        if (! snapshot.isEmpty) {
                            val documents = snapshot.documents
                            postList.clear()
                            for (document in documents) {
                                val post = document.toObject(Post::class.java)
                                val archivePost = document.get("isArchive") as Boolean?
                                if (archivePost == false || archivePost == null) {
                                    if (post!=null) postList.add(post)
                                }
                            }
                            recyclerViewAdapter.notifyDataSetChanged()
                        }
                        else {
                            // Snapshot boş, yani post yok
                            binding.emptyPost.visibility = View.VISIBLE
                        }
                    }
                }
            }

    }
}