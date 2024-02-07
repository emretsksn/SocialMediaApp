package com.emretaskesen.tpost.ui.fragment.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentLoginBinding
import com.emretaskesen.tpost.ui.activity.main.MainActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class LoginFragment : Fragment() {

    private lateinit var binding : FragmentLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentLoginBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.loginpageToolbar)
        val myActionBar : androidx.appcompat.app.ActionBar? =
               (requireActivity() as AppCompatActivity).supportActionBar
        myActionBar?.run {
            setDisplayShowTitleEnabled(true)
        }


        binding.loginButton.setOnClickListener {
            loginFunc()
        }

        binding.registerButton.setOnClickListener {
            loadFragment(RegisterFragment())
        }
        binding.forgotPasswordText.setOnClickListener {
            loadFragment(ResetPasswordFragment())
        }
    }

    private fun loadFragment(fragment : Fragment) {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView , fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun loginFunc() {
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()

        if (email.isBlank() || password.isBlank()) {
            val messages = getString(R.string.title_controldata)
            Toast.makeText(requireActivity(),messages, Toast.LENGTH_LONG).show()
        }
        else {
            signInUser(email , password)
        }
    }

    private fun signInUser(email : String , password : String) {
        auth.signInWithEmailAndPassword(email , password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser !!
                if (user.isEmailVerified) {
                    binding.loadingGif2.visibility = View.GONE
                    joinTimeListener()
                    val currentUser = auth.currentUser?.displayName.toString()
                    val message = getString(R.string.welcome_text , currentUser)
                    Toast.makeText(requireActivity(),message, Toast.LENGTH_LONG).show()
                    requireActivity().finish()
                    startActivity(Intent(requireActivity() , MainActivity::class.java))

                }
                else {
                    binding.loadingGif2.visibility = View.GONE
                    auth.signOut()
                    val message = getString(R.string.message_notverification)
                    Toast.makeText(requireActivity(),message, Toast.LENGTH_LONG).show()
                }
            }else{
                Timber.tag("LogInFailed").e(task.toString())
            }
        }.addOnFailureListener { e->
            when(e.localizedMessage){
                "The supplied auth credential is incorrect, malformed or has expired." ->{
                    Toast.makeText(requireActivity(),"Giriş bilgileriniz hatalı. Lütfen kontrol ediniz.",Toast.LENGTH_LONG).show()
                }
                "The email address is badly formatted." ->{
                    binding.emailText.error = "Geçerli e-posta adresi giriniz."
                }
            }
            e.localizedMessage?.let { Timber.tag("LogInFailed2").e(it) }
        }
    }

    private fun joinTimeListener() {
        val currentUser = auth.currentUser !!
        val userID = currentUser.uid

        val userDocument = db.collection("Users").document(userID)

        val joinMap = hashMapOf<String , Any>()
        val joinTime = Timestamp.now()
        joinMap["joinTime"] = joinTime
        userDocument.update(joinMap)
    }
}