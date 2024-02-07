package com.emretaskesen.tpost.ui.fragment.user

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
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentAccountSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AccountSettingFragment : Fragment() {
    private var _binding: FragmentAccountSettingBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    var userID: String? = null
    val db = Firebase.firestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAccountSettingBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMenu()
        userID = arguments?.getString("userID")
        initViewsAndFunctions()

    }

    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home ->{
                        requireActivity().supportFragmentManager.popBackStack()
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }

    private fun initViewsAndFunctions(){
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.settingAccountToolbar)
        val myActionBar : ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar
        myActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.savePassword.setOnClickListener{
            updatePrivateInfo()
        }

    }



    private fun updatePrivateInfo() {
        val currentUser = auth.currentUser!!
        val password = binding.passwordText.text.toString()
        val passwordConfirm = binding.passwordText2.text.toString()
        val password2Input = binding.passwordInput2
        when {
            passwordConfirm.isEmpty() -> {
                password2Input.error = getString(R.string.title_errorconfirmpassword)
            }
        }
        when {
            password != passwordConfirm -> {
                password2Input.error = getString(R.string.password_not_match)
            }
        }
        when (password) {
            passwordConfirm -> {
                currentUser.updatePassword(password).addOnCompleteListener {
                        val message = requireActivity().getString( R.string.update_password_toast_text)
                    Toast.makeText(context,message, Toast.LENGTH_LONG).show()

                    }.addOnFailureListener { exception ->
                    Toast.makeText(context,exception.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}