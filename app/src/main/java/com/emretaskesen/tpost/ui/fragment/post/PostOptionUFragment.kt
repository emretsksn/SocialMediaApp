package com.emretaskesen.tpost.ui.fragment.post

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentPostOptionUBinding
import com.emretaskesen.tpost.ui.activity.post.EditPost
import com.emretaskesen.tpost.ui.activity.post.PostDetail
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class PostOptionUFragment : BottomSheetDialogFragment() {

    private lateinit var binding : FragmentPostOptionUBinding
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(
            postID : String ,
            userID : String ,
            postImage : ArrayList<String>? ,
            callerIntent : Int
        ) : PostOptionUFragment {
            val fragment = PostOptionUFragment()
            val args = Bundle()
            args.putString("postID" , postID)
            args.putString("userID" , userID)
            args.putStringArrayList("postImage" , postImage)
            args.putInt("callerIntent",callerIntent)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentPostOptionUBinding.inflate(inflater , container , false)
        return binding.root

    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()

    }

    private fun initViewsAndFunctions() {
        val postID : String? = arguments?.getString("postID")
        val userID : String? = arguments?.getString("userID")
        val callerIntent : Int? = arguments?.getInt("callerIntent")
        val myID = auth.currentUser?.uid
        var isSave = false
        var isArchive = false
        val postImages : ArrayList<String>? = arguments?.getStringArrayList("postImage")
        if (! postImages.isNullOrEmpty()) {
            binding.pmImageSave.visibility = View.VISIBLE
        }
        val drawableLeft1 : Drawable? =
            ContextCompat.getDrawable(requireActivity() , R.drawable.ic_bookmark_add)
        val drawableLeft2: Drawable? = ContextCompat.getDrawable(requireActivity() , R.drawable.ic_bookmark_remove)
        val drawableLeft3: Drawable? = ContextCompat.getDrawable(requireActivity() , R.drawable.ic_archive_add)
        val drawableLeft4: Drawable? = ContextCompat.getDrawable(requireActivity() , R.drawable.ic_archive_remove)
        val saveRef = db.collection("Users").document(myID!!).collection("SavePost").document(postID!!)
        saveRef.addSnapshotListener { value, error ->
            if (error!=null){
                return@addSnapshotListener
            }else{
                if (value!=null){
                    val postId = value.get("postID") as String?
                    if (postID == postId){
                        isSave = true
                        binding.pmSave.setText(R.string.save_comleted)
                        binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft2 ,  // Sol (start) drawable
                            null ,       // Üst drawable
                            null ,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }else{
                        isSave = false
                        binding.pmSave.setText(R.string.save)
                        binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft1 ,  // Sol (start) drawable
                            null ,       // Üst drawable
                            null ,       // Sağ (end) drawable
                            null        // Alt drawable
                        )
                    }
                }
            }
        }
        db.collection("Post").document(postID).addSnapshotListener { value, error ->
            if (error!=null){
                return@addSnapshotListener
            }else{
                if (value!=null) {
                    val archivePost = value.get("isArchive") as Boolean?
                    if (archivePost == true) {
                        isArchive = true
                        binding.pmArchive.setText(R.string.remove_archive)
                        binding.pmArchive.setCompoundDrawablesWithIntrinsicBounds(drawableLeft4 ,  // Sol (start) drawable
                            null ,       // Üst drawable
                            null ,       // Sağ (end) drawable
                            null
                        )

                    }else{
                        isArchive = false
                        binding.pmArchive.setText(R.string.archive)
                        binding.pmArchive.setCompoundDrawablesWithIntrinsicBounds(
                            drawableLeft3 ,  // Sol (start) drawable
                            null ,       // Üst drawable
                            null ,       // Sağ (end) drawable
                            null
                        )
                    }
                }
            }
        }
        binding.pmSave.setOnClickListener {
            if (isSave){
                val myUidSave = db.collection("Users").document(myID).collection("SavePost")
                    .document(postID)
                myUidSave.delete().addOnSuccessListener {
                    isSave = false
                    binding.pmSave.text = getString(R.string.save)
                    binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft1 ,  // Sol (start) drawable
                        null ,       // Üst drawable
                        null ,       // Sağ (end) drawable
                        null        // Alt drawable
                    )
                }

            }else{
                val myUidSave = db.collection("Users").document(myID).collection("SavePost")
                val saveMap = HashMap<String , Any>()
                saveMap["likeTime"] = Timestamp.now()
                saveMap["postID"] = postID
                saveMap["userID"] = userID!!
                myUidSave.document(postID).set(saveMap).addOnSuccessListener {
                    isSave = true
                    binding.pmSave.text = getString(R.string.save_comleted)
                    binding.pmSave.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft2 ,  // Sol (start) drawable
                        null ,       // Üst drawable
                        null ,       // Sağ (end) drawable
                        null        // Alt drawable
                    )
                }
            }
        }

        val archiveRef = db.collection("Post").document(postID)
        binding.pmArchive.setOnClickListener {
            if (isArchive){
                val archiveMap = HashMap<String,Any>()
                archiveMap["isArchive"] = false
                archiveRef.update(archiveMap).addOnSuccessListener {
                    isArchive = false
                    binding.pmArchive.setText(R.string.archive)
                    binding.pmArchive.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft3 ,  // Sol (start) drawable
                        null ,       // Üst drawable
                        null ,       // Sağ (end) drawable
                        null
                    )
                    dismiss()
                }
            }else{
                val archiveMap = HashMap<String,Any>()
                archiveMap["isArchive"] = true
                archiveRef.update(archiveMap).addOnSuccessListener {
                    isArchive = true
                    binding.pmArchive.setText(R.string.remove_archive)
                    binding.pmArchive.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft4 ,  // Sol (start) drawable
                        null ,       // Üst drawable
                        null ,       // Sağ (end) drawable
                        null
                    )
                    dismiss()
                }
            }
        }

        if (callerIntent==2){
            binding.pmView.visibility = View.GONE
        }else{
            binding.pmView.visibility = View.VISIBLE
        }
        binding.pmView.setOnClickListener {
            dismiss()
            val intent = Intent(requireActivity() , PostDetail::class.java)
            intent.putExtra("postID" , postID)
            intent.putExtra("userID" , userID)
            requireActivity().startActivity(intent)
        }

        binding.pmEditpost.setOnClickListener {
            dismiss()
            val intent = Intent(activity , EditPost::class.java)
            intent.putExtra("postID" , postID)
            requireActivity().startActivity(intent)
        }

        binding.pmDeletepost.setOnClickListener {
            val alertDialog = AlertDialog.Builder(requireActivity())
            alertDialog.setTitle(R.string.delete_post)
            alertDialog.setMessage(R.string.delete_post_message)
            alertDialog.setPositiveButton(R.string.approve) { _ , _ ->
                deletePost(postID)
            }
            alertDialog.setNegativeButton(R.string.cancel) { dialog , _ ->
                dialog.cancel()
            }
            alertDialog.show()
            dismiss()
        }
        binding.pmImageSave.setOnClickListener {
            if (! postImages.isNullOrEmpty()) {
                postImages.forEach { imageLink ->
                    Glide.with(requireActivity()).asBitmap().load(imageLink) // Non-null Firebase'den gelen link
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource : Bitmap ,
                                transition : Transition<in Bitmap>? ,
                            ) {
                                saveImageToGallery(requireActivity() , resource , postID)

                            }

                            override fun onLoadCleared(placeholder : Drawable?) {
                            }
                        })
                }
            }
        }

    }
    fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME , "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE , "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING , 1)
            }
        }

        val contentResolver = context.contentResolver

        // Resmi MediaStore'a ekle
        val imageUri : Uri? = contentResolver.insert(imageCollection , contentValues)

        imageUri?.let { uri ->
            // Resmi kaydet
            contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , outputStream)
                }
            }

            // Android Q ve üzeri için resmi "işlemde" olarak işaretleme işlemini tamamla
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING , 0)
                contentResolver.update(uri , contentValues , null , null)
            }
        }
        return imageUri
    }

    private fun deletePost(postID : String) {
        val images : ArrayList<String>? = arguments?.getStringArrayList("postImage")
        val reference =  db.collection("Post").document(postID)
        reference.delete().addOnSuccessListener {
            images?.forEach { uri ->
                deletePostImage(uri)
            }
            dismiss()
        }
    }

    private fun deletePostImage(imageUrl : String) {
        val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl)
        storageReference.delete()

    }
}