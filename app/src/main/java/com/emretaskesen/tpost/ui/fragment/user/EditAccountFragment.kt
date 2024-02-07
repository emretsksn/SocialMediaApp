package com.emretaskesen.tpost.ui.fragment.user

import android.app.NotificationManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.FragmentEditAccountBinding
import com.emretaskesen.tpost.ui.activity.user.EditUserProfile
import com.emretaskesen.tpost.ui.fragment.images.SelectSourceFragment
import com.emretaskesen.tpost.util.ConstVal
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage

class EditAccountFragment : Fragment() , SelectSourceFragment.BottomSheetListener {
    private var _binding : FragmentEditAccountBinding? = null
    private val binding get() = _binding !!
    private val db = FirebaseFirestore.getInstance()
    private var selectedImage : Uri? = null
    private var isProfilePhoto : Boolean? = null
    private val storage = Firebase.storage
    private val auth = FirebaseAuth.getInstance()
    private lateinit var userDocument : DocumentReference
    private var gender: String? = null

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        _binding = FragmentEditAccountBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initMenu()
        val myId = auth.currentUser?.uid !!
        userDocument = db.collection("Users").document(myId)
        initViewsAndFunctions()
        getMyData(myId)
        dataBaseUser()

    }
    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_edit_profie , menu)

            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        updatePersonalData()
                        true
                    }
                    android.R.id.home ->{
                        requireActivity().finish()
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }


    private fun dataBaseUser() {
        userDocument.update("isNew" , false)
    }

    private fun initViewsAndFunctions() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.editAccountToolbar)
        val myActionBar : ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar
        myActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.genderMale.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                gender = buttonView.text.toString()
            }
        }
        binding.genderFemale.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                gender = buttonView.text.toString()
            }
        }
        binding.genderNo.setOnCheckedChangeListener { buttonView , isChecked ->
            if (isChecked) {
                gender = buttonView.text.toString()
            }
        }
        binding.editImageText.setOnClickListener {
            selectedImage = null
            val selectSource = SelectSourceFragment.newInstance(1)
            selectSource.setBottomSheetListener(this)
            val tag = SelectSourceFragment::class.java.simpleName
            selectSource.show(requireActivity().supportFragmentManager , tag)
        }
        binding.changePassword.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.editUserContainer , AccountSettingFragment())
            transaction.commit()
        }
    }

    private fun getMyData(userID : String) {
        UserCache.getUser(userID) { userModel ->
            userModel?.let { user ->
                binding.personelNameText.setText(user.personalName)
                binding.profileunEditText.setText(user.userName)
                binding.profilebioEditText.setText(user.biography)
                binding.profileLinkEditText.setText(user.userLink)
                binding.birthDayText.setText(user.birthday)
                Glide.with(this@EditAccountFragment).load(user.profileImage)
                    .error(R.drawable.ic_rounded_user).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.myPhoto)
                when (user.userGender){
                    "Erkek" -> {
                        binding.genderMale.isChecked = true
                    }
                    "Kadın" -> {
                        binding.genderFemale.isChecked = true
                    }
                    "Belirtme" -> {
                        binding.genderNo.isChecked = true
                    }
                    else -> {
                        binding.genderNo.isChecked = true
                    }
                }
            } ?: run {
                UserCache.clear()
                return@getUser
            }
        }
    }


    private fun updatePersonalData() {
        val currentUser = auth.currentUser !!
        val inputName = binding.profileunEditText
        val personalName = binding.personelNameText
        val biography = binding.profilebioEditText
        if (!inputName.text.isNullOrBlank()&&!personalName.text.isNullOrBlank()&&!biography.text.isNullOrBlank()){
            when {
                inputName.text.toString().any { it.isUpperCase() || it.isWhitespace() } -> {
                    inputName.error = "Büyük harf veya boşluk kullanılamaz."
                }
                else -> {
                    val updateProfile = userProfileChangeRequest {
                        if (!inputName.text.isNullOrBlank()) {
                            displayName = inputName.text.toString()
                        }
                    }
                    currentUser.updateProfile(updateProfile).addOnSuccessListener {
                        updateData(personalName.text.toString() , biography.text.toString())
                        val message = getString(R.string.message_updatesuccesful)
                        Toast.makeText(requireActivity(),message, Toast.LENGTH_LONG).show()


                    }.addOnFailureListener { e ->
                        Toast.makeText(requireActivity(),e.localizedMessage,Toast.LENGTH_LONG).show()
                    }
                }
            }
        }



    }

    private fun updateData(personalName : String , biography : String) {
        val currentUser = auth.currentUser !!
        val userName = currentUser.displayName !!
        val userProfile = currentUser.photoUrl
        val userLink = binding.profileLinkEditText.text.toString()
        val birthDay = binding.birthDayText.text.toString()
        val userMap = HashMap<String , Any>()
        userMap["userName"] = userName
        if (personalName.isNotBlank()) {
            userMap["personalName"] = personalName
        }
        if (biography.isNotBlank()) {
            userMap["biography"] = biography
        }
        if (gender!=null) {
            userMap["userGender"] = gender!!
        }
        if (userProfile != null) {
            userMap["profileImage"] = userProfile
        }
        if (userLink.isNotBlank()) {
            userMap["userLink"] = userLink
        }
        if (birthDay.isNotBlank()) {
            userMap["birthday"] = birthDay
        }
        userDocument.update(userMap).addOnSuccessListener {
            UserCache.clear()
            getMyData(currentUser.uid)
            sentFinishNotify()
        }

    }

    private fun uploadProfileUri(uri : String){
        val currentUser = auth.currentUser !!
        userDocument.update("profileImage",uri).addOnSuccessListener {
            UserCache.clear()
            getMyData(currentUser.uid)
        }
    }

    private fun updateProfileImage(profilePhoto : Uri) {
        val storageRef = storage.reference
        val currentUser = auth.currentUser !!
        val userMail = currentUser.email.toString()
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        val imagesRef = storageRef.child("profileImage")
        val profileImage = imagesRef.child(userMail)
        val uploadTask = profileImage.putFile(profilePhoto,metadata)

        uploadTask.addOnProgressListener { taskSnapshot->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            binding.loadingProgress.visibility = View.VISIBLE
            binding.loadingProgress.progress = progress.toInt()
            binding.loadingLevel.visibility = View.VISIBLE
            binding.loadingLevel.text =
                getString(R.string.image_uploading , progress.toInt().toString())
        }
        uploadTask.addOnPausedListener { taskSnapshot->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            binding.loadingProgress.progress = progress.toInt()
        }
        uploadTask.addOnSuccessListener {
            binding.loadingLevel.text =
                getString(R.string.image_uploaded)
            profileImage.downloadUrl.addOnSuccessListener { uri ->
                val updateProfile = userProfileChangeRequest {
                        photoUri = uri
                }
                currentUser.updateProfile(updateProfile).addOnSuccessListener {
                    uploadProfileUri(uri.toString())
                    UserCache.clear()
                    getMyData(currentUser.uid)
                    binding.loadingProgress.visibility = View.GONE
                    binding.loadingLevel.visibility = View.GONE
                }.addOnFailureListener { e ->
                    Toast.makeText(requireActivity(),e.localizedMessage,Toast.LENGTH_LONG).show()
                }
            }
        }
        uploadTask.addOnFailureListener{ e ->
            Toast.makeText(requireActivity(),e.localizedMessage,Toast.LENGTH_LONG).show()
        }
    }

    private fun updateProfileBackground(background : Uri) {
        val storageRef = storage.reference
        val currentUser = auth.currentUser !!
        val userMail = currentUser.email.toString()
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        val imagesRef = storageRef.child("profileBackground")
        val backgroundImage = imagesRef.child(userMail)
        val uploadTask = backgroundImage.putFile(background,metadata)

        uploadTask.addOnProgressListener { taskSnapshot->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            binding.loadingProgress.visibility = View.VISIBLE
            binding.loadingProgress.progress = progress.toInt()
            binding.loadingLevel.visibility = View.VISIBLE
            binding.loadingLevel.text =
                getString(R.string.image_uploading , progress.toInt().toString())
        }
        uploadTask.addOnPausedListener { taskSnapshot->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            binding.loadingProgress.progress = progress.toInt()
        }
        uploadTask.addOnSuccessListener {
            binding.loadingLevel.text =
                getString(R.string.image_uploaded)
            backgroundImage.downloadUrl.addOnSuccessListener { uri ->
                val url = uri.toString()
                userDocument.update("profileBackground" , url)
                UserCache.clear()
                getMyData(currentUser.uid)
                binding.loadingProgress.visibility = View.GONE
                binding.loadingLevel.visibility = View.GONE
            }
        }
        uploadTask.addOnFailureListener{ e ->
            Toast.makeText(requireActivity(),e.localizedMessage,Toast.LENGTH_LONG).show()
        }
    }


    private fun sentFinishNotify() {

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(requireActivity(),
            ConstVal.Notifications.CHANNEL_ID)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_manage_accounts)
            .setContentTitle("Profil Güncellendi")
            .setSound(defaultSoundUri)
            .build()

        val notificationManager = requireActivity().getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onBottomSheetResult(data : Uri) {
        updateProfileImage(data)
        binding.myPhoto.setImageURI(data)
    }

}