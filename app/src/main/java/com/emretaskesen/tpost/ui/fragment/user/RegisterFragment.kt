package com.emretaskesen.tpost.ui.fragment.user

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentRegisterBinding
import com.emretaskesen.tpost.ui.fragment.images.SelectSourceFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.util.Locale

class RegisterFragment : Fragment() , SelectSourceFragment.BottomSheetListener {

    private val storage : StorageReference = Firebase.storage.reference
    private var selectedImage : Uri? = null
    private lateinit var binding : FragmentRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var mailAddress : TextInputEditText
    private lateinit var password : TextInputEditText
    private lateinit var userName : TextInputEditText
    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentRegisterBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initViewsAndFunctions()
    }

    override fun onResume() {
        super.onResume()
        userNameSearch()
    }

    private fun initViewsAndFunctions() {
        mailAddress = binding.emailText
        password = binding.passwordText
        userName = binding.usernameText
        setupUsernameInput()
        setupEmailInput()
        setupPasswordInput()
        binding.registerButton.setOnClickListener {
            registerNow()
        }
        binding.editImageText.setOnClickListener {
            val selectSource = SelectSourceFragment.newInstance(1)
            selectSource.setBottomSheetListener(this)
            val tag = SelectSourceFragment::class.java.simpleName
            selectSource.show(requireActivity().supportFragmentManager , tag)
        }

        binding.registerToolbar.setNavigationIcon(R.drawable.ic_arrow_left)
        binding.registerToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }


    // Kayıt ol butonu fonksiyon ve fotoğrafın veritabanına kaydedilmesi
    private fun registerNow() {
        val messages = getString(R.string.title_controldata)
        if (mailAddress.text.isNullOrBlank() && password.text.isNullOrBlank() && userName.text.isNullOrBlank()) {
            Toast.makeText(requireActivity() , messages , Toast.LENGTH_LONG).show()
        } else {
            if (selectedImage != null) {
                binding.loadingGif2.visibility = View.VISIBLE
                val reference = storage.child("profileImage").child(mailAddress.toString())
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()

                reference.putFile(selectedImage !! , metadata).addOnSuccessListener {
                    reference.downloadUrl.addOnSuccessListener { uri ->
                        register(uri)
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireActivity() , e.localizedMessage , Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                register(null)
            }
        }
    }

    // Kullanıcının oluşturulması
    private fun register(photoUrl : Uri?) {
        val messages = getString(R.string.title_controldata)
        if (mailAddress.text.isNullOrBlank() && password.text.isNullOrBlank() && userName.text.isNullOrBlank()) {
            Toast.makeText(requireActivity() , messages , Toast.LENGTH_LONG).show()
        } else {
            auth.setLanguageCode("tr")
            auth.createUserWithEmailAndPassword(mailAddress.toString() , password.toString())
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser
                        val userNameUpdate = userProfileChangeRequest {
                            displayName = userName.text.toString()
                            photoUri = photoUrl
                        }

                        currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    val message = getString(R.string.message_updatesuccesful)
                                    Toast.makeText(requireActivity() , message , Toast.LENGTH_LONG)
                                        .show()
                                } else {
                                    val message = verificationTask.exception?.localizedMessage
                                    Toast.makeText(requireActivity() , message , Toast.LENGTH_LONG)
                                        .show()
                                }
                            }

                        currentUser?.updateProfile(userNameUpdate)?.addOnCompleteListener {
                            dataBaseUser(currentUser)
                        }
                    } else {
                        val message =
                            "Kullanıcı oluşturulurken beklenmedik hatalar oluştu. Tekrar deneyiniz."
                        Toast.makeText(requireActivity() , message , Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { e ->
                    when (e.localizedMessage) {
                        "The email address is already in use by another account." -> {
                            binding.emailText.error =
                                "E-posta adresi zaten başka bir hesap tarafından kullanılıyor."
                        }

                        "The email address is badly formatted." -> {
                            binding.emailText.error = "E-posta adresi kötü biçimlendirilmiş."
                        }
                    }
                    e.localizedMessage?.let { Log.e("RegisterFailed" , it) }
                    Toast.makeText(requireActivity() , e.localizedMessage , Toast.LENGTH_LONG)
                        .show()
                }
        }

    }


    private fun dataBaseUser(currentUser : FirebaseUser) {
        val usersMap = HashMap<String , Any>()
        usersMap["userID"] = currentUser.uid
        usersMap["userName"] = currentUser.displayName.toString()
        usersMap["userMail"] = currentUser.email.toString()
        usersMap["profileImage"] = currentUser.photoUrl.toString()
        usersMap["accountStatus"] = true
        usersMap["onlineStatus"] = true
        usersMap["isNew"] = true
        db.collection("Users").document(currentUser.uid).set(usersMap).addOnSuccessListener {
            val userNameMap = HashMap<String , Any>()
            userNameMap["userName"] = currentUser.displayName.toString()
            userNameMap["userID"] = currentUser.uid
            db.collection("UserName").document(currentUser.uid).set(userNameMap)
                .addOnSuccessListener {
                    binding.loadingGif2.visibility = View.GONE
                    auth.signOut()
                    requireActivity().supportFragmentManager.popBackStack()
                }
        }

    }

    // Kullanıcı adı filtreleme
    private fun setupUsernameInput() {
        var isTextWatcherActive = false

        userName.addTextChangedListener(object : TextWatcher {

            var previousText = ""

            override fun beforeTextChanged(
                s : CharSequence ,
                start : Int ,
                count : Int ,
                after : Int ,
            ) {
                if (! isTextWatcherActive) {
                    previousText = s.toString()
                }
            }

            override fun onTextChanged(
                s : CharSequence ,
                start : Int ,
                before : Int ,
                count : Int ,
            ) {
                if (isTextWatcherActive) {
                    return
                }
                isTextWatcherActive = true
                val currentText = s.toString()
                when {
                    currentText.any { it.isUpperCase() } -> {
                        userName.setText(currentText.lowercase(Locale.ROOT))
                        userName.setSelection(currentText.length)
                    }

                    currentText.any { it.isWhitespace() } -> {
                        userName.setText(getString(R.string.username_whitespace_control ,
                            previousText))
                        userName.setSelection(currentText.length)
                    }

                    else -> {
                        userName.error = null
                    }
                }
                userNameSearch()
                isTextWatcherActive = false
            }

            override fun afterTextChanged(s : Editable?) {
                userNameSearch()
            }
        })

    }

    private fun userNameSearch() {
        val inputName = userName.text.toString()
        db.collection("UserName").addSnapshotListener { value , error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (value != null) {
                    if (! value.isEmpty) {
                        for (userDoc in value.documents) {
                            val userNames = userDoc.get("userName") as String
                            if (userNames == inputName) {
                                userName.error = "Kullanıcı adı mevcut!"
                            } else {
                                userName.error = null
                            }
                        }
                    }
                }
            }
        }
    }

    // Email filtreleme
    private fun setupEmailInput() {
        mailAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s : CharSequence? ,
                start : Int ,
                count : Int ,
                after : Int ,
            ) {
            }

            override fun onTextChanged(
                s : CharSequence ,
                start : Int ,
                before : Int ,
                count : Int ,
            ) {
                val currentText = s.toString()
                if (! Patterns.EMAIL_ADDRESS.matcher(currentText)
                        .matches() && currentText.isNotEmpty()
                ) {
                    mailAddress.error = "Lütfen geçerli bir e-posta adresi giriniz"
                } else {
                    mailAddress.error = null
                }
            }

            override fun afterTextChanged(s : Editable?) {}
        })
    }

    //Parola filtreleme
    private fun setupPasswordInput() {
        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s : CharSequence? ,
                start : Int ,
                count : Int ,
                after : Int ,
            ) {
            }

            override fun onTextChanged(
                s : CharSequence ,
                start : Int ,
                before : Int ,
                count : Int ,
            ) {
                when (before > 8) {
                    true -> {
                        password.error = null
                    }

                    else -> {
                        password.error = "Parola en az 8 haneli olmalıdır!"
                    }
                }
                val currentText = s.toString()
                when {
                    ! currentText.any { it.isDigit() } -> {
                        password.error = "Parola en az bir rakam içermelidir!"
                    }

                    ! currentText.any { it.isUpperCase() } -> {
                        password.error = "Parola en az bir büyük harf içermelidir!"
                    }

                    ! currentText.any { it.isLowerCase() } -> {
                        password.error = "Parola en az bir küçük harf içermelidir!"
                    }

                    else -> {
                        password.error = null
                    }
                }
            }

            override fun afterTextChanged(s : Editable?) {
            }
        })

    }

    override fun onBottomSheetResult(data : Uri) {
        selectedImage = data
        binding.myPhoto.setImageURI(data)
    }


}