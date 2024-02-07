package com.emretaskesen.tpost.ui.fragment.images

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.emretaskesen.tpost.util.CropImage
import com.emretaskesen.tpost.databinding.FragmentSelectSourceBinding
import com.emretaskesen.tpost.model.CropSettings
import com.emretaskesen.tpost.ui.activity.images.CameraActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.util.UUID

class SelectSourceFragment : BottomSheetDialogFragment() {
    private var _binding : FragmentSelectSourceBinding? = null
    private val binding get() = _binding !!
    private var listener: BottomSheetListener? = null
    private var photoType: Int? = null
    var uri:Uri? =null
    interface BottomSheetListener {
        fun onBottomSheetResult(data: Uri)
    }

    fun setBottomSheetListener(listener: BottomSheetListener) {
        this.listener = listener
    }


    private val getCamera =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == Activity.RESULT_OK){
                val resultUri = result.data?.getStringExtra("selectedImage")?.toUri()
                val destinationUri = Uri.fromFile(createUniqueFile())
                resultUri?.let {
                    when (photoType){
                        1 ->{
                            val cropSettings = CropSettings(
                                sourceUri = resultUri,
                                destinationUri = destinationUri,
                                aspectRatioX = 1f,
                                aspectRatioY = 1f
                            )
                            cropLauncher.launch(cropSettings)
                        }
                        2 ->{
                            val cropSettings = CropSettings(
                                sourceUri = resultUri,
                                destinationUri = destinationUri,
                                aspectRatioX = 12f,
                                aspectRatioY = 4f
                            )
                            cropLauncher.launch(cropSettings)
                        }

                        else -> {
                            sendDataToActivity(resultUri)
                        }
                    }

                }
            }
        }

    private val getGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.data
            val destinationUri = Uri.fromFile(createUniqueFile())
            resultUri?.let {
                when (photoType){
                    1 ->{
                        val cropSettings = CropSettings(
                            sourceUri = resultUri,
                            destinationUri = destinationUri,
                            aspectRatioX = 1f,
                            aspectRatioY = 1f
                        )
                        cropLauncher.launch(cropSettings)
                    }
                    2 ->{
                        val cropSettings = CropSettings(
                            sourceUri = resultUri,
                            destinationUri = destinationUri,
                            aspectRatioX = 12f,
                            aspectRatioY = 4f
                        )
                        cropLauncher.launch(cropSettings)
                    }

                    else -> {
                        sendDataToActivity(resultUri)
                    }
                }
            }
        }
    }

    private val cropLauncher =
        registerForActivityResult(CropImage()) {croppedUri ->
            croppedUri ?: return@registerForActivityResult // If null, return
            sendDataToActivity(croppedUri)
        }

    companion object {
        fun newInstance(photoType: Int): SelectSourceFragment {
            val fragment = SelectSourceFragment()
            val args = Bundle()
            args.putInt("photoType",photoType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        _binding = FragmentSelectSourceBinding.inflate(inflater , container , false)
        return binding.root

    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        val photoTypeRet = arguments?.getInt("photoType")
        if (photoTypeRet!=null){
            photoType = photoTypeRet
        }
        initViewsAndFunctions()
    }

    private fun createUniqueFile(): File {
        val fileName = "cropped_${UUID.randomUUID()}.jpg" // Generate a unique file name
        return File(requireActivity().cacheDir, fileName)
    }
    private fun initViewsAndFunctions() {
        binding.selectCamera.setOnClickListener{
            startCamera()
        }
        binding.selectGallery.setOnClickListener {
            chooseImage()
        }
    }

    private fun startCamera(){
        val intent = Intent(requireContext(), CameraActivity::class.java)
        getCamera.launch(intent)
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        getGallery.launch(intent)
    }

    private fun sendDataToActivity(data: Uri) {
        listener?.onBottomSheetResult(data)
    }

    override fun onDestroy() {
        super.onDestroy()
        photoType = null
        _binding = null
    }
}