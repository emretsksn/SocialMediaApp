package com.emretaskesen.tpost.ui.fragment.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.MessageListAdapter
import com.emretaskesen.tpost.databinding.FragmentMessagesBinding
import com.emretaskesen.tpost.model.MessageList
import com.emretaskesen.tpost.ui.fragment.message.AddMessageFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore


class MessagesFragment : Fragment(){

    // View'lara erişebilmek için binding nesnesi
    private var _binding : FragmentMessagesBinding? = null
    var auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    private val binding get() = _binding !!
    val profileImage = FirebaseAuth.getInstance().currentUser?.photoUrl
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
    lateinit var recyclerViewAdapter : MessageListAdapter
    private var messageList = arrayListOf<MessageList>()


    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        // ui bağlantısı
        _binding = FragmentMessagesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        setupToolbar()
        initMenu()
        initViewsAndFunctions()
    }


    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.myMessageList.layoutManager = layoutManager
        recyclerViewAdapter = MessageListAdapter(requireActivity() , messageList)
        binding.myMessageList.adapter = recyclerViewAdapter
        getMyMessage()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.mymessageToolbar)
        (requireActivity() as AppCompatActivity).supportActionBar
    }

    override fun onResume() {
        super.onResume()
        getMyMessage()
    }

    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_messages_fragment , menu)

            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.mpAddMessage -> {
                        val addMessageFragment = AddMessageFragment()
                        val tag = AddMessageFragment::class.java.simpleName
                        addMessageFragment.show(requireActivity().supportFragmentManager,tag)
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getMyMessage() {
        val myID = auth.currentUser?.uid
        db.collection("Users").document(myID !!).collection("MyMessages")
            .orderBy("messageTime" , Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots , _ ->
                messageList.clear()
                val documents = snapshots?.documents
                if (documents != null) {
                    for (messageDoc in documents) {
                        val userID = messageDoc.getString("userID") ?: ""
                        val messageCount = messageDoc.getBoolean("messageCount")
                        val lastMessage = messageDoc.getString("lastMessage") ?: ""
                        val isGroup = messageDoc.getBoolean("isGroup")
                        val myMessage = MessageList(
                            userID , messageCount , lastMessage,isGroup
                        )
                        messageList.add(myMessage)
                    }
                    recyclerViewAdapter.notifyDataSetChanged()
                }
            }
    }


}