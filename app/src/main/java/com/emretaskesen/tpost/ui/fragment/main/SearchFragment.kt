package com.emretaskesen.tpost.ui.fragment.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.ui.adapter.PostAdapter
import com.emretaskesen.tpost.ui.adapter.UsersAdapter
import com.emretaskesen.tpost.databinding.FragmentSearchBinding
import com.emretaskesen.tpost.model.Post
import com.emretaskesen.tpost.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchFragment:  Fragment() {
    private lateinit var binding : FragmentSearchBinding

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userAdapter : UsersAdapter
    private var userList = ArrayList<UserModel>()
    private lateinit var postAdapter : PostAdapter
    private var postList = arrayListOf<Post>()
    private var auth = FirebaseAuth.getInstance()


    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        // ui bağlantısı
        binding = FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()
        getFirebaseData()
        getUsers()
    }

    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        val layoutManager2 = LinearLayoutManager(requireActivity())

        binding.userDetail.layoutManager = layoutManager2
        userAdapter = UsersAdapter(requireActivity() , userList)
        binding.userDetail.adapter = userAdapter

        binding.discorverPostDetail.layoutManager = layoutManager
        postAdapter = PostAdapter(requireActivity() , postList)
        binding.discorverPostDetail.adapter = postAdapter
        binding.idSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query : String?) : Boolean {
                if (! query.isNullOrEmpty()) {
                    filterUsers(query)
                    filterPost(query)
                }
                return true
            }

            override fun onQueryTextChange(newText : String?) : Boolean {
                filterUsers(newText)
                filterPost(newText)
                return true
            }
        })
    }

    // Arama yapmak için bu metodu kullanın
    private fun filterUsers(query : String?) {
        if (! query.isNullOrEmpty()) {
            val filterUsername = userList.filter {
                it.userName?.contains(query) == true
            }
            val filterPersonalName = userList.filter {
                it.personalName?.contains(query) == true
            }
            binding.userDetail.visibility = View.VISIBLE
            userAdapter.filterList(filterUsername , filterPersonalName)
        }
        else {
            binding.userDetail.visibility = View.GONE
        }
    }

    private fun filterPost(query : String?) {
        if (! query.isNullOrEmpty()) {
            val filteredList = postList.filter {
               it.postContent?.contains(query) == true
            }
            postAdapter.updateData(filteredList)
        }
        else {
            postAdapter.updateData(postList)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUsers() {
        val myID = FirebaseAuth.getInstance().currentUser?.uid
        db.collection("Users").orderBy("userName" , Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot , error ->
                if (error != null) {
                    Toast.makeText(activity , error.localizedMessage , Toast.LENGTH_LONG).show()
                }
                else {
                    snapshot?.let {
                        userList.clear()
                        if (! it.isEmpty) {
                            val documents = snapshot.documents
                            for (userDoc in documents) {
                                val userID = userDoc.getString("userID") ?: ""
                                if (userID != myID) {
                                    val user = userDoc.toObject(UserModel::class.java)
                                    if (user != null) {
                                        userList.add(user)
                                    }
                                }
                                userAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getFirebaseData() {
        val myID = auth.currentUser?.uid
        db.collection("Post").orderBy("postDate" , Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot , error ->
                if (error != null) {
                    Toast.makeText(requireActivity() , error.localizedMessage , Toast.LENGTH_LONG)
                        .show()
                }
                else {
                    if (snapshot != null) {
                        if (! snapshot.isEmpty) {
                            val documents = snapshot.documents
                            postList.clear()
                            for (document in documents) {
                                val userID = document.get("userID") as String?
                                val privatePost = document.get("privatePost") as Boolean?
                                val archivePost = document.get("isArchive") as Boolean?

                                val post = document.toObject(Post::class.java)
                                if (archivePost == false || archivePost == null) {
                                    if (privatePost == false || privatePost == null || userID == myID) {
                                        if (post != null) {
                                            postList.add(post)
                                        }
                                    }
                                }

                            }
                            postAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
    }


}