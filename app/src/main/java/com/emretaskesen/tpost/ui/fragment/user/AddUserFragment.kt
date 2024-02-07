package com.emretaskesen.tpost.ui.fragment.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.AddUserAdapter
import com.emretaskesen.tpost.databinding.FragmentAddUserBinding
import com.emretaskesen.tpost.model.UserModel
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.google.firebase.firestore.FirebaseFirestore

class AddUserFragment : DialogFragment(), AddUserAdapter.OnUserListener {
    private var listener: BottomSheetListener? = null
    interface BottomSheetListener {
        fun onBottomSheetResult(userID: String,userName : String)
    }

    fun setBottomSheetListener(listener: BottomSheetListener) {
        this.listener = listener
    }

    private fun sendDataToActivity(userID: String,userName : String) {
        listener?.onBottomSheetResult(userID,userName)
    }
    var userList = arrayListOf<UserModel>()
    private lateinit var adapter: AddUserAdapter
    private lateinit var binding: FragmentAddUserBinding
    private val db = FirebaseFirestore.getInstance()


    companion object {
        fun newInstance(callerIntent : Int): AddUserFragment {
            val fragment = AddUserFragment()
            val args = Bundle()
            args.putInt("callerIntent",callerIntent)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        initViewsAndFunctions()
        getUsers()
    }


    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.searchUser.layoutManager = layoutManager
        adapter = AddUserAdapter(requireContext(), userList, this)
        binding.searchUser.adapter = adapter
        binding.idSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query : String?) : Boolean {
                if (! query.isNullOrEmpty()) {
                    filterUsers(query)
                }
                return true
            }

            override fun onQueryTextChange(newText : String?) : Boolean {
                filterUsers(newText)
                return true
            }
        })

        binding.userprofileToolbar.setNavigationIcon(R.drawable.ic_close)
        binding.userprofileToolbar.setNavigationOnClickListener {
            dismiss()
        }

    }


    private fun filterUsers(query: String?) {
        if (!query.isNullOrEmpty()) {
            val filteredList = userList.filter {
                it.userName?.contains(query) == true
            }
            adapter.filterList(filteredList)
        } else {
            adapter.filterList(userList)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getUsers() {
        db.collection("Users").get().addOnSuccessListener { userSnapshot ->
            userList.clear()
            for (userDoc in userSnapshot) {
                val users = userDoc.toObject(UserModel::class.java)
                userList.add(users)
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onUserClik(user: UserModel , view: View) {

        val callerIntent : Int? = arguments?.getInt("callerIntent")
        if (callerIntent==1){
            val intent = Intent(requireContext(), ChatPage::class.java).apply {
                putExtra("userID", user.userID)
            }
            requireContext().startActivity(intent)
            dialog?.dismiss()
        }else{
            user.userID?.let {
                user.userName?.let { it1 -> sendDataToActivity(user.userID !! , it1) }
            }
        }
    }
}