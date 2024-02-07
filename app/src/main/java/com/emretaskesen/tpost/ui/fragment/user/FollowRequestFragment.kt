package com.emretaskesen.tpost.ui.fragment.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.ui.adapter.RequestAdapter
import com.emretaskesen.tpost.databinding.FragmentFollowRequestBinding
import com.emretaskesen.tpost.model.UserModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FollowRequestFragment : BottomSheetDialogFragment(), RequestAdapter.OnRequestListener {

    private lateinit var binding: FragmentFollowRequestBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerViewAdapter: RequestAdapter
    private var requestList = arrayListOf<UserModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFollowRequestBinding.inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewsAndFunctions()
        getRequest()
    }

    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.followRequestDetail.layoutManager =
            layoutManager
        recyclerViewAdapter = RequestAdapter(requireActivity(), requestList, this)
        binding.followRequestDetail.adapter =
            recyclerViewAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getRequest(){
        val myID = auth.currentUser?.uid
        if (myID!=null){
            val uidFollowR =
                db.collection("Users").document(myID).collection("FollowRequest")

            uidFollowR.orderBy("requestTime",Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
                if (error!=null){
                    return@addSnapshotListener
                }else{
                    if (snapshot!=null){
                        if (!snapshot.isEmpty){
                            val documents = snapshot.documents
                            for (document in documents){
                                val user = document.toObject(UserModel::class.java)
                                if (user!=null)requestList.add(user)
                            }
                            recyclerViewAdapter.notifyDataSetChanged()
                        }else{
                            return@addSnapshotListener
                        }
                    }
                }
            }
        }

    }

    override fun onRequestClick(request: UserModel , view: View) {
        dismiss()
    }

}