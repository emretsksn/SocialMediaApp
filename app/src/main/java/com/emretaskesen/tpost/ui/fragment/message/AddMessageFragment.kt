package com.emretaskesen.tpost.ui.fragment.message

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.ui.adapter.AddUserAdapter
import com.emretaskesen.tpost.databinding.FragmentAddMessageBinding
import com.emretaskesen.tpost.model.UserModel
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.emretaskesen.tpost.ui.activity.message.GroupChat
import com.emretaskesen.tpost.ui.fragment.images.SelectSourceFragment
import com.emretaskesen.tpost.ui.fragment.user.AddUserFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddMessageFragment : BottomSheetDialogFragment(), AddUserAdapter.OnUserListener, SelectSourceFragment.BottomSheetListener {
    private var listener: BottomSheetListener? = null
    interface BottomSheetListener {
        fun onBottomSheetResult(userID: String,userName : String)
    }

    fun setBottomSheetListener(listener: BottomSheetListener) {
        this.listener = listener
    }

    private var userList = arrayListOf<UserModel>()
    private lateinit var adapter: AddUserAdapter
    private lateinit var binding: FragmentAddMessageBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImage : Uri? = null
    private val selectedUserIDs = arrayListOf<String>()
    private val groupID = UUID.randomUUID().toString()
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
        binding = FragmentAddMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewsAndFunctions()
        getUsers()
    }


    private fun initViewsAndFunctions() {
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
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
        binding.sentMessageButton.setOnClickListener {
            if (selectedUserIDs.size == 1){
                val intent = Intent(requireContext(), ChatPage::class.java).apply {
                    putExtra("userID", selectedUserIDs[0])
                }
                startActivity(intent)
            }else{
                if (binding.editGroupName.text.isNullOrEmpty()){
                    binding.editGroupName.error = "Grup Adı Giriniz"
                }else{
                    saveDatabase()
                }

            }
        }
        binding.groupImage.setOnClickListener {
            selectedImage = null
            val selectSource = SelectSourceFragment.newInstance(1)
            selectSource.setBottomSheetListener(this)
            val tag = SelectSourceFragment::class.java.simpleName
            selectSource.show(requireActivity().supportFragmentManager , tag)
        }

    }

    private fun createGroup(photoUrl:String?){
        val groupName = binding.editGroupName.text.toString()
        val groupMap = HashMap<String , Any>()
        if (!photoUrl.isNullOrEmpty()){
            groupMap["groupPhoto"] = photoUrl
        }
        groupMap["groupName"] = groupName
        groupMap["groupID"] = groupID
        groupMap["groupMembers"] = selectedUserIDs
        db.collection("Groups").document(groupID).set(groupMap).addOnSuccessListener {
            val myID = auth.currentUser !!.uid
            selectedUserIDs.add(myID)

            selectedUserIDs.forEach { userIDs->
                messageCounterUpdate(userIDs,groupID)
            }
            val intent = Intent(requireContext(), GroupChat::class.java).apply {
                putExtra("groupID",groupID)
            }
            startActivity(intent)
        }
    }

    private fun messageCounterUpdate(userID : String,groupID:String) {
        val myID = auth.currentUser !!.uid
        val userName = auth.currentUser?.displayName
        val groupName = binding.editGroupName.text.toString()
        val messageMap = HashMap<String , Any>()
        messageMap["userID"] = groupID
        messageMap["messageTime"] = Timestamp.now()
        messageMap["isGroup"] = true
        messageMap["lastMessage"] =  "$userName $groupName grubunu kurdu."
        val counterMap = HashMap<String , Any>()
        counterMap["messageTime"] = Timestamp.now()
        counterMap["userID"] = groupID
        counterMap["isGroup"] = true
        counterMap["lastMessage"] =  "$userName $groupName grubunu kurdu."
        db.collection("Users").document(userID).collection("MyMessages").document(groupID).set(counterMap)
            .addOnSuccessListener {
                db.collection("Users").document(myID).collection("MyMessages").document(groupID)
                    .set(messageMap).addOnSuccessListener {

                    }
            }

    }
    private fun saveDatabase() {
        when {
            selectedImage != null -> { // If the user added an image, save it to storage and call the firestore save function
                val reference = FirebaseStorage.getInstance().reference
                val imageReference = reference.child("messageImage").child(groupID)
                imageReference.putFile(selectedImage !!).addOnSuccessListener {
                    val uploadImageReference = reference.child("messageImage").child(groupID)
                    uploadImageReference.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        createGroup(downloadUrl)
                    }

                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext() ,
                        exception.localizedMessage ,
                        Toast.LENGTH_LONG).show()
                }
            }

            else -> { // If no image is added, call the firestore registration function directly
                createGroup(null)
            }
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

    override fun onUserClik(user : UserModel , view : View) {
        if (user.userName!=null&&user.userID!=null){
            when {
                selectedUserIDs.size < 10 -> {
                    if (selectedUserIDs.contains(user.userID)){
                        Toast.makeText(requireContext() , "Kullanıcı Mevcut" , Toast.LENGTH_SHORT)
                            .show()
                    }else{
                        selectedUserIDs.add(user.userID!!)
                        addUserChip(user.userID!!)
                        when {
                            selectedUserIDs.size > 1 -> {
                                binding.editGroupName.visibility = View.VISIBLE
                                binding.sentMessage.visibility = View.VISIBLE
                                binding.groupImage.visibility = View.VISIBLE
                            }
                            selectedUserIDs.size == 1 -> {
                                binding.editGroupName.visibility = View.GONE
                                binding.sentMessage.visibility = View.VISIBLE
                                binding.groupImage.visibility = View.GONE
                            }
                            selectedUserIDs.isEmpty() ->{
                                binding.sentMessage.visibility = View.GONE
                            }
                        }
                    }
                }
                else -> { // Liste dolu olduğunda yapılacak işlemler (örneğin kullanıcıyı bilgilendirme)
                    Toast.makeText(requireContext() , "Gruba en fazla 5 kişi eklenebilir!" , Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun addUserChip(userID : String) {
        val chipGroup = binding.addUser
        val chip : Chip = Chip(requireActivity()).apply {
            UserCache.getUser(userID) { userModel ->
                userModel?.let { user ->
                    text = context.getString(R.string.user_name_tag , user.userName)
                }
            }
            isCloseIconVisible = true
            textSize = 11F
            setTextColor(context.getColorStateList(R.color.text_color))
            backgroundTintList = context.getColorStateList(R.color.button_background)
            chipBackgroundColor = context.getColorStateList(R.color.button_background)
            setOnCloseIconClickListener {
                chipGroup.removeView(this)
                selectedUserIDs.remove(userID) // Bu ID'yi listenden çıkar (userID'yi elde etmeniz gerekecek)
                if (selectedUserIDs.size == 0){
                    binding.sentMessage.visibility = View.GONE
                }
            }
        }
        chipGroup.addView(chip)
    }

    override fun onBottomSheetResult(data : Uri) {
        binding.groupImage.setImageURI(data)
        selectedImage = data
    }
}
