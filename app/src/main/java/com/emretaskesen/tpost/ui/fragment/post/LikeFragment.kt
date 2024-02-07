package com.emretaskesen.tpost.ui.fragment.post

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.ui.adapter.LikeAdapter
import com.emretaskesen.tpost.databinding.FragmentLikeBinding
import com.emretaskesen.tpost.model.LikeModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class LikeFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentLikeBinding
    lateinit var recyclerViewAdapter: LikeAdapter
    private var likeList = arrayListOf<LikeModel>()
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(postID: String): LikeFragment {
            val fragment = LikeFragment()
            val args = Bundle()
            args.putString("post_id", postID)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLikeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postID = arguments?.getString("post_id")
        postID?.let {
            getLike(it)
        } ?: run {
            Toast.makeText(context, "Post ID bulunamadı.", Toast.LENGTH_LONG).show()
        }
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.likeDetail.layoutManager = layoutManager
        recyclerViewAdapter = LikeAdapter(requireContext(), likeList)
        binding.likeDetail.adapter = recyclerViewAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getLike(postID: String) {
        db.collection("Post").document(postID).collection("Like")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                } else {
                    if (snapshot != null) {
                        if (!snapshot.isEmpty){
                            val documents = snapshot.documents
                            for (commentD in documents) {
                                val userID = commentD.get("userID") as String?
                                val likeTime = commentD.get("likeTime") as Timestamp?
                                val downloadLike = LikeModel(
                                    userID, likeTime
                                )
                                likeList.add(downloadLike)
                            }
                            recyclerViewAdapter.notifyDataSetChanged()
                        }else {
                            binding.emptyLikeBanner.visibility = View.VISIBLE
                        }
                    }
                }
            }

    }
}