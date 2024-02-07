package com.emretaskesen.tpost.ui.fragment.post

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.CommentAdapter
import com.emretaskesen.tpost.databinding.FragmentCommentsBinding
import com.emretaskesen.tpost.model.CommentModel
import com.emretaskesen.tpost.util.ConstVal.Configurations.GIPHY_API_KEY
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.util.UUID

class CommentFragment : BottomSheetDialogFragment(), CommentAdapter.OnCommentListener, GiphyDialogFragment.GifSelectionListener {
    private lateinit var binding: FragmentCommentsBinding
    lateinit var recyclerViewAdapter: CommentAdapter
    private var commentList = arrayListOf<CommentModel>()
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    private val myId = auth.currentUser?.uid
    var postID: String? = null
    private var selectedGif: Uri? = null

    companion object {
        fun newInstance(postID: String,userID:String): CommentFragment {

            val fragment = CommentFragment()
            val args = Bundle()
            args.putString("postID", postID)
            args.putString("userID",userID)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dialog = dialog as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        val postID = arguments?.getString("postID")
        val userID = arguments?.getString("userID")
        if (!userID.isNullOrBlank()&&!postID.isNullOrBlank()){
            initViewsAndFunctions()
        }else{
            Toast.makeText(requireActivity(), "Post ID bulunamadı.", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    private fun initViewsAndFunctions(){
        val postID = arguments?.getString("postID")
        val userID = arguments?.getString("userID")
        if (!userID.isNullOrBlank()&&!postID.isNullOrBlank()){
            getComment(postID)
            textControl()
            val layoutManager = LinearLayoutManager(requireContext())
            layoutManager.stackFromEnd = true
            binding.commentDetail.layoutManager = layoutManager
            recyclerViewAdapter = CommentAdapter(requireContext(),commentList,this)
            binding.commentDetail.adapter = recyclerViewAdapter
            Giphy.configure(requireActivity() , GIPHY_API_KEY)
            binding.commentSent.setOnClickListener {
                val commentText = binding.commentText.text.toString()
                sendComment(postID,commentText,userID)
            }
            binding.addGif.setOnClickListener {
                showGiphyDialog()
            }
            binding.commentText.setOnClickListener {
                binding.cardEmojiArea.visibility = View.GONE
            }

            val myEditText: EditText = binding.commentText
            /* val inputMethodManager = binding.addEmoji.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager*/
            binding.addEmoji.setOnClickListener {
                // Klavyeyi kontrol etmek için kullanılacak InputMethodManager örneği alınıyor.
                val inputMethodManager = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                // Eğer EditText şu anda odaklanmış değilse klavyeyi göster.
                if (!myEditText.isFocused) {
                    myEditText.requestFocus() // EditText odaklanmasını sağlar.

                    // Klavyeyi göster
                    inputMethodManager.showSoftInput(myEditText, InputMethodManager.SHOW_IMPLICIT)
                    binding.cardEmojiArea.visibility = View.GONE
                } else {
                    // Eğer EditText zaten odaklanmışsa, klavyeyi kapat.
                    inputMethodManager.hideSoftInputFromWindow(myEditText.windowToken, 0)
                    binding.cardEmojiArea.visibility = View.VISIBLE
                }
            }
            binding.emojiPicker.setOnEmojiPickedListener {
                binding.commentText.append(it.emoji)
            }
        }

    }

    private fun showGiphyDialog() {
        val giphyDialog = GiphyDialogFragment.newInstance()
        giphyDialog.gifSelectionListener = this
        giphyDialog.show(requireActivity().supportFragmentManager , "giphy_dialog")
    }
    private fun textControl() {
        binding.commentText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s : CharSequence? ,
                start : Int ,
                count : Int ,
                after : Int ,
            ) {
            }

            override fun onTextChanged(
                s : CharSequence? ,
                start : Int ,
                before : Int ,
                count : Int
            ) {
            }

            override fun afterTextChanged(s : Editable?) {
                // This method is called after the text has changed.
                // Here we control the visibility of sendButton.
                if (s.isNullOrBlank()) {
                    binding.commentSent.isEnabled = false
                    binding.addGif.visibility = View.VISIBLE
                    binding.commentSent.setImageResource(R.drawable.ic_message_send_passive)
                }
                else {
                    binding.commentSent.isEnabled = true
                    binding.commentSent.setImageResource(R.drawable.ic_message_send_active)
                    binding.addGif.visibility = View.GONE
                }
            }
        })
    }
    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getComment(postID: String){
        db.collection("Post").document(postID).collection("Comment").orderBy("commentTime", Query.Direction.DESCENDING).addSnapshotListener{ snapshot, e->
            if (e!=null){
                return@addSnapshotListener
            }else{
                if (snapshot!=null){
                    if (!snapshot.isEmpty){
                        commentList.clear()
                        val documents = snapshot.documents
                        for (commentD in documents){
                            val userID = commentD.get("userID") as String?
                            val commentID = commentD.get("commentID") as String?
                            val commentTime = commentD.get("commentTime") as Timestamp?
                            val commentContent = commentD.get("commentContent") as String?
                            val downloadComment = CommentModel(
                                userID = userID,
                                commentID = commentID,
                                postID = postID,
                                commentContent = commentContent,
                                commentTime = commentTime
                            )
                            commentList.add(downloadComment)
                        }
                        recyclerViewAdapter.notifyDataSetChanged()
                    }else{
                       binding.emptyCommentBanner.visibility = View.VISIBLE
                    }

                }

            }
        }

    }

    private fun sendComment(postID: String, comment:String,userID:String){
        val documentID = UUID.randomUUID().toString()
        val commentMap = HashMap<String,Any>()
        commentMap["userID"] = myId!!
        commentMap["commentID"] = documentID
        commentMap["commentTime"] = Timestamp.now()
        commentMap["commentContent"] = comment
        if (selectedGif!=null){
            commentMap["commentGif"] = selectedGif!!
        }
        db.collection("Post").document(postID).collection("Comment").document(documentID).set(commentMap).addOnSuccessListener {
            createNotification(postID,userID)
        }
    }

    private fun createNotification(postID:String,userID: String) {
        val myID = auth.currentUser!!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val documentPath = UUID.randomUUID().toString()
        val notifMap = HashMap<String,Any>()
        notifMap["notificationID"] = documentPath
        notifMap["notificationType"] = "commentNotification"
        notifMap["notificationTitle"] = "@$myName paylaşımına yorum yaptı"
        notifMap["notificationContent"] = binding.commentText.text.toString()
        notifMap["postID"] = postID
        notifMap["userID"] = userID
        notifMap["notificationTime"] = Timestamp.now()
        notifMap["trackID"] = myID
        notifMap["isNew"] = true
        notifRef.document(documentPath).set(notifMap).addOnSuccessListener {
            db.collection("Users").document(userID).collection("Follower").get()
                .addOnSuccessListener {
                    binding.commentText.setText("")
                }
        }
    }

    override fun onCommentClick(comment: CommentModel , view: View) {
        if (comment.commentID!=null){
            db.collection("Post").document(comment.postID!!).collection("Comment").document(comment.commentID!!).delete().addOnSuccessListener {
                Toast.makeText(requireContext(),"Yorum Silindi",Toast.LENGTH_LONG).show()
                commentList.removeIf { it.commentID == comment.commentID}
                recyclerViewAdapter.notifyDataSetChanged()
            }
        }

    }
    // Giphy actions
    override fun didSearchTerm(term : String) {
        // You can write here what to do when the search term is entered in the search bar.
    }

    override fun onDismissed(selectedContentType : GPHContentType) {
        // You can write here what to do when the Giphy dialog is closed.
    }

    override fun onGifSelected(
        media : Media ,
        searchTerm : String? ,
        selectedContentType : GPHContentType ,
    ) {

    }

}