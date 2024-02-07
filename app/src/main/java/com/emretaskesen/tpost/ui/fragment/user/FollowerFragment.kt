package com.emretaskesen.tpost.ui.fragment.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.model.UserModel
import com.emretaskesen.tpost.ui.adapter.UsersAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class FollowerFragment : Fragment() {

    private lateinit var userAdapter : UsersAdapter
    var userList = arrayListOf<UserModel>()
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    var userID: String? = null

    companion object {
        fun newInstance(userID: String): FollowerFragment {
            val fragment = FollowerFragment()
            val args = Bundle()
            args.putString("userID", userID)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userID = arguments?.getString("userID")
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_follower, container, false)
        val commentDetail = view.findViewById<RecyclerView>(R.id.followerDetail)

        val layoutManager = LinearLayoutManager(requireContext())
        commentDetail.layoutManager = layoutManager
        userAdapter = UsersAdapter(requireContext(),userList)
        commentDetail.adapter = userAdapter

        userID?.let {
            getUser(it)
        }

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getUser(userID: String) {
        db.collection("Users").document(userID).collection("Follower")
            .addSnapshotListener { snapshot , e ->
                if (e != null) {
                    return@addSnapshotListener
                } else snapshot?.let {
                    userList.clear()
                    if (! it.isEmpty) {
                        val documents = snapshot.documents
                        for (userDoc in documents) {
                            val myID = auth.currentUser?.uid
                            val userIDs = userDoc.get("userID") as String?
                            if (userIDs!=myID){
                                userList.add(userDoc.toObject(UserModel::class.java)!!)
                            }
                        }
                        userAdapter.notifyDataSetChanged()
                    }
                }

            }
    }
}
