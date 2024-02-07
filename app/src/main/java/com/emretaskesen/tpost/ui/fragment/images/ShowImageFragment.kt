package com.emretaskesen.tpost.ui.fragment.images

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.databinding.FragmentShowImageBinding


class ShowImageFragment : DialogFragment() {
    private var _binding: FragmentShowImageBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(imageUrl : Uri?): ShowImageFragment {

            val fragment = ShowImageFragment()
            val args = Bundle()
            args.putString("imageUrl",imageUrl.toString())
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Dialog'u oluştur
        val dialog = super.onCreateDialog(savedInstanceState)
        // Tam ekran görünüm için stil ayarla
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }
    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        // ui bağlantısı
        _binding = FragmentShowImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val imageUrl = arguments?.getString("imageUrl")
        showImage(imageUrl!!)
        binding.closePage.setOnClickListener {
            dismissNow()
        }
    }


    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    }


    private fun showImage(imageUrl : String){
        Glide.with(requireContext())
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.fullscreenImageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}