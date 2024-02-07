package com.emretaskesen.tpost.ui.fragment.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.emretaskesen.tpost.BuildConfig.GIPHY_API_KEY
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.FragmentAddPostBinding
import com.emretaskesen.tpost.ui.activity.main.MainActivity
import com.emretaskesen.tpost.ui.activity.main.MapsActivity
import com.emretaskesen.tpost.ui.activity.post.PostDetail
import com.emretaskesen.tpost.ui.fragment.user.AddUserFragment
import com.emretaskesen.tpost.ui.fragment.images.SelectSourceFragment
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.emretaskesen.tpost.util.Notify
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import pl.droidsonroids.gif.GifImageView
import timber.log.Timber
import java.util.UUID

class AddPostFragment : BottomSheetDialogFragment() , GiphyDialogFragment.GifSelectionListener ,
    SelectSourceFragment.BottomSheetListener , AddUserFragment.BottomSheetListener {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val selectedUserIDs = mutableListOf<String>()
    private val selectedImages = mutableListOf<Uri>()
    private val downLoadedUri = mutableListOf<Uri>()
    private val uuid : UUID = UUID.randomUUID()
    private var latLng : LatLng? = null
    private var locationName : String? = null
    private var _binding : FragmentAddPostBinding? = null
    private val binding get() = _binding !!


    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        _binding = FragmentAddPostBinding.inflate(inflater , container , false)
        return binding.root

    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        val dialog = dialog as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        val inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        @Suppress("DEPRECATION") inputMethodManager.showSoftInput(view ,
            InputMethodManager.SHOW_FORCED)
        Giphy.configure(requireActivity() , GIPHY_API_KEY)
        initViewsAndFunctions()
    }

    @Suppress("DEPRECATION")
    private val getLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
               latLng = result.data !!.getParcelableExtra("latLang")
                locationName = result.data !!.getStringExtra("locationName")
                binding.addedLocation.visibility = View.VISIBLE
                binding.addedLocation.text = locationName // Verileri istediğiniz gibi kullanın
                if (latLng != null && locationName != null) {
                    Timber.tag("LocationResult").d("$locationName + $latLng")
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        selectedUserIDs.clear()
        selectedImages.clear()
        downLoadedUri.clear()
    }

    private fun initViewsAndFunctions() {

        binding.postpageToolbar.setNavigationIcon(R.drawable.ic_close)
        binding.postpageToolbar.setNavigationOnClickListener {
            dismiss()
        }
        binding.addImageButton.setOnClickListener {
            val selectSource = SelectSourceFragment()
            selectSource.setBottomSheetListener(this)
            val tag = SelectSourceFragment::class.java.simpleName
            selectSource.show(requireActivity().supportFragmentManager , tag)
        }
        binding.addGifButton.setOnClickListener {
            showGiphyDialog()
        }
        binding.addUserTagButton.setOnClickListener {
            val addUserFragment = AddUserFragment()
            addUserFragment.setBottomSheetListener(this)
            val tag = AddUserFragment::class.java.simpleName
            addUserFragment.show(requireActivity().supportFragmentManager , tag)
        }
        binding.addLocation.setOnClickListener {
            getLocation.launch(Intent(requireActivity() , MapsActivity::class.java))
        }

        binding.sharePost.setOnClickListener {
            sharePost()
            startActivity(Intent(requireActivity() , MainActivity::class.java))
        }

        Glide.with(requireActivity()).load(auth.currentUser?.photoUrl)
            .error(R.drawable.ic_rounded_user).into(binding.userProfile)
    }


    private fun showGiphyDialog() {
        val giphyDialog = GiphyDialogFragment.newInstance()
        giphyDialog.gifSelectionListener = this
        giphyDialog.show(requireActivity().supportFragmentManager , "giphy_dialog")
    }

    private fun sharePost() {
        if (selectedImages.isNotEmpty()) {
            val uploadTasks = mutableListOf<Task<Uri>>()
            selectedImages.forEach { imageUri ->
                val reference = storage.reference
                val imageName = Timestamp.now().toString()
                val imageReference =
                    reference.child("postImages").child(uuid.toString()).child(imageName)
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
                val uploadTask =
                    imageReference.putFile(imageUri , metadata).continueWithTask { task ->
                        if (! task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        imageReference.downloadUrl
                    }.addOnSuccessListener { uri ->
                        downLoadedUri.add(uri)
                    }.addOnFailureListener { exception ->
                        Toast.makeText(requireActivity() ,
                            exception.localizedMessage ,
                            Toast.LENGTH_LONG).show()
                    }

                uploadTasks.add(uploadTask)
            }

            binding.loadingProgress.visibility = View.VISIBLE

            Tasks.whenAllSuccess<Uri>(uploadTasks).addOnSuccessListener {
                    binding.loadingProgress.visibility = View.GONE
                    saveToDataBase() // Tüm resimlerin yüklenmesi tamamlanınca, veritabanına kaydedin.
                }.addOnFailureListener { exception ->
                    binding.loadingProgress.visibility = View.GONE
                    Toast.makeText(requireActivity() ,
                        exception.localizedMessage ,
                        Toast.LENGTH_LONG).show()
                }
        } else {
            saveToDataBase() // Hiç resim yüklenmeyecekse direkt olarak veritabanına kaydedin.
        }
    }

    private fun saveToDataBase() {
        val postContent = binding.postContent.text.toString()
        val userID = auth.currentUser?.uid.toString()
        val currentTime = Timestamp.now()
        val documentPath = uuid.toString()
        val postDocument = db.collection("Post").document(documentPath)
        val postMap = hashMapOf<String , Any>()
        postMap["userID"] = userID
        postMap["postID"] = documentPath
        postMap["postDate"] = currentTime

        if (selectedUserIDs.isNotEmpty()) {
            postMap["taggedUserIDs"] = selectedUserIDs
        }
        if (postContent.isNotBlank()) {
            postMap["postContent"] = postContent
        }
        if (downLoadedUri.isNotEmpty()) {
            postMap["postImages"] = downLoadedUri
        }

        if (latLng != null) {
            postMap["postLocation"] = latLng !!
            postMap["locationName"] = locationName !!
        }
        val post = hashMapOf<String , Any>()
        post[documentPath] = documentPath
        postDocument.set(postMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                selectedUserIDs.forEach { userID ->
                    createNotification(documentPath , userID)
                }
                val intent = Intent(requireActivity() , PostDetail::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("postID" , documentPath)
                    putExtra("userID" , userID)
                }
                Notify(requireActivity()).sentFinishNotify(requireActivity() ,
                    "Paylaşım Yapıldı" ,
                    "Kontrol Et" ,
                    R.drawable.ic_all_post ,
                    intent)
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireActivity() , exception.localizedMessage , Toast.LENGTH_LONG)
                .show()
        }


    }

    private fun createNotification(postID : String , userID : String) {
        val myID = auth.currentUser !!.uid
        val notifRef = db.collection("Users").document(userID).collection("Notification")
        val myName = auth.currentUser?.displayName
        val documentPath = UUID.randomUUID().toString()
        val notifMap = HashMap<String , Any>()
        notifMap["notificationID"] = documentPath
        notifMap["notificationType"] = "tagNotification"
        notifMap["notificationTitle"] = "$myName sizi bir gönderiye etiketledi."
        notifMap["notificationContent"] = binding.postContent.text.toString()
        notifMap["postID"] = postID
        notifMap["userID"] = myID
        notifMap["notificationTime"] = Timestamp.now()
        notifMap["trackID"] = myID
        notifMap["isNew"] = true
        notifRef.document(documentPath).set(notifMap).addOnSuccessListener {}
    }

    override fun didSearchTerm(term : String) { // Arama terimi arama çubuğuna girildiğinde ne yapılacağını buraya yazabilirsiniz.
    }

    override fun onDismissed(selectedContentType : GPHContentType) { // Giphy dialog kapatıldığında ne yapılacağını buraya yazabilirsiniz.
    }

    override fun onGifSelected(
        media : Media ,
        searchTerm : String? ,
        selectedContentType : GPHContentType ,
    ) { // Kullanıcı bir GIF seçtiğinde ne yapılacağını buraya yazabilirsiniz.
        val uri = Uri.parse(media.images.fixedWidth?.gifUrl)

        // Resmi göstermek için ImageView'inizi güncelleyin.
        uri?.let { imageUri ->
            val size = downLoadedUri.size.plus(selectedImages.size)
            if (size < 5) {
                downLoadedUri.add(imageUri)
                addImage(imageUri)
            } else {
                Toast.makeText(requireActivity() ,
                    "En fazla 5 görsel desteklenmektedir." ,
                    Toast.LENGTH_SHORT).show()
            }

        }

        // Gif URL'sini bir değişkene ata ve daha sonra Firestore'a kaydetmek için kullan.
        // ...
        // Burada gereken Firestore kaydetme işlemlerini yapın.
    }

    override fun onBottomSheetResult(data : Uri) {
        val size = downLoadedUri.size.plus(selectedImages.size)
        if (size < 5) {
            selectedImages.add(data)
            addImage(data)
        } else {
            Toast.makeText(requireActivity() ,
                "En fazla 5 görsel desteklenmektedir." ,
                Toast.LENGTH_SHORT).show()
        }

    }

    override fun onBottomSheetResult(userID : String , userName : String) {

        when {
            selectedUserIDs.size < 10 -> {
                when {
                    selectedUserIDs.contains(userID) -> {
                        Toast.makeText(requireContext() ,
                            R.string.user_available , Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(requireContext() ,
                            getString(R.string.user_mentioned , userName) , Toast.LENGTH_SHORT).show()
                        selectedUserIDs.add(userID)
                        addUserChip(userID)
                    }
                }

            }
            else -> { // Liste dolu olduğunda yapılacak işlemler (örneğin kullanıcıyı bilgilendirme)
                Toast.makeText(requireActivity() ,
                    R.string.maximum_10_users_added , Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun addImage(imageUrl : Uri) {
        val imagesArea = binding.imagesArea
        val imageWidth = 72.dpToPx()
        val imageHeight = 72.dpToPx()
        val images = GifImageView(requireActivity()).apply {
            Glide.with(this).load(imageUrl).into(this)
            layoutParams = ViewGroup.LayoutParams(imageWidth , imageHeight)
            setOnClickListener {
                val showImageFragment =
                    ShowImageFragment.newInstance(imageUrl) // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                showImageFragment.show((it.context as FragmentActivity).supportFragmentManager ,
                    showImageFragment.tag)
            }
            setOnLongClickListener { view ->
                val popupMenu = PopupMenu(requireActivity() , view)
                popupMenu.inflate(R.menu.menu_postpageother)
                popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(item : MenuItem?) : Boolean {
                        when (item?.itemId) {
                            R.id.pp_delete -> {
                                imagesArea.removeView(this@apply)
                                selectedImages.remove(imageUrl)
                                return true
                            }
                        }
                        return false
                    }
                })
                popupMenu.show()
                true
            }
        }

        imagesArea.addView(images)
    }

    private fun Int.dpToPx() : Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun addUserChip(userID : String) {
        val chipGroup = binding.taggedUsers
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
            }
        }
        chipGroup.addView(chip)
    }

}