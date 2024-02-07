package com.emretaskesen.tpost.ui.fragment.images

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.GalleryAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File


class  GalleryFragment : BottomSheetDialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        recyclerView = view.findViewById(R.id.galleryDetail)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = GalleryAdapter(requireContext(), listOf())
        recyclerView.adapter = adapter
        loadImages()
        return view
    }

    private fun loadImages() {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")

        cursor?.let {
            while (it.moveToNext()) {
                val columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val imagePath = cursor.getString(columnIndexData)
                images.add(Uri.fromFile(File(imagePath)))
            }
            cursor.close()
        }

        adapter.updateImages(images)
    }


    companion object {
        private const val PERMISSIONS_REQUEST_READ_STORAGE = 101
    }
}