package com.emretaskesen.tpost.ui.fragment.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentResetPasswordBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ResetPasswordFragment : Fragment() {

    private lateinit var binding : FragmentResetPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        binding = FragmentResetPasswordBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)

        binding.resetToolbar.setNavigationIcon(R.drawable.ic_arrow_left)
        binding.resetToolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.resetButton.setOnClickListener {
            val emailAddress = binding.emailText.text.toString()
            auth.setLanguageCode("tr")
            Firebase.auth.sendPasswordResetEmail(emailAddress).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            requireActivity() ,
                            R.string.password_reset_mail_send ,
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireActivity() , e.localizedMessage , Toast.LENGTH_SHORT)
                        .show()
                }

        }
    }



}