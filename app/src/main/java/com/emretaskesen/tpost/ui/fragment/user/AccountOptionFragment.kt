package com.emretaskesen.tpost.ui.fragment.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.input.key.Key.Companion.I
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.FragmentAccountOptionBinding
import com.emretaskesen.tpost.ui.activity.post.ActionPost
import com.emretaskesen.tpost.ui.activity.user.EditUserProfile
import com.emretaskesen.tpost.ui.activity.user.Auth
import com.emretaskesen.tpost.ui.activity.main.MainActivity
import com.emretaskesen.tpost.ui.activity.settings.AppSettings
import com.emretaskesen.tpost.ui.fragment.post.PostOptionFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import java.lang.Exception

class AccountOptionFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAccountOptionBinding
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(postID: String, userID: String): PostOptionFragment {
            val fragment = PostOptionFragment()
            val args = Bundle()
            args.putString("postID",postID)
            args.putString("userID",userID)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAccountOptionBinding.inflate(inflater , container , false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewsAndFunctions()
    }

    @Suppress("DEPRECATION")
    private fun initViewsAndFunctions() {
        binding.editProfile.setOnClickListener {
            val intent = Intent(requireActivity(), EditUserProfile::class.java).apply {
                putExtra("getType",1)
            }
            requireActivity().startActivity(intent)
            
            dismiss()
        }
        binding.settingButton.setOnClickListener {
            requireActivity().startActivity(Intent(requireActivity(),AppSettings::class.java))
            
            dismiss()
        }
        binding.savedPost.setOnClickListener {
            val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                putExtra("getType",1)
            }
            requireActivity().startActivity(intent)
            
            dismiss()
        }
        binding.likedPost.setOnClickListener {
            val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                putExtra("getType",2)
            }
            requireActivity().startActivity(intent)
            
            dismiss()
        }
        binding.archivedPost.setOnClickListener {
            val intent = Intent(requireActivity(), ActionPost::class.java).apply {
                putExtra("getType",3)
            }
            requireActivity().startActivity(intent)
            
            dismiss()
        }
        binding.logOut.setOnClickListener {
            logOut()
            dismiss()
        }
    }

    private fun logOut() {
        val signOutAlert = androidx.appcompat.app.AlertDialog.Builder(requireActivity())
        signOutAlert.setTitle(R.string.sign_out_alert_title)
        signOutAlert.setMessage(R.string.sign_out_alert_message)
        signOutAlert.setIcon(R.drawable.ic_log_out)
        signOutAlert.setPositiveButton(
            R.string.approve
        ) { _ , _ ->
            try {
                FirebaseAuth.getInstance().signOut()
            }catch (e: Exception){
                e.localizedMessage?.let { Timber.tag("LogOutFailed").e(it) }
            }finally {
                MainActivity().finish()
                requireActivity().startActivity(Intent(activity, Auth::class.java))
            }
        }

        signOutAlert.setNeutralButton(
            R.string.cancel
        ) { _ , _ ->
            val message = requireActivity().getString(R.string.sign_out_toast_text)
            Toast.makeText(activity,message, Toast.LENGTH_LONG).show()
        }
        signOutAlert.show()
        dismiss()
    }
}