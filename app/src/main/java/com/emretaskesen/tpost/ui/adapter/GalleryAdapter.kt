package com.emretaskesen.tpost.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emretaskesen.tpost.R

//Kamera aktivitesinde çekilden fotoğraların fragmentte listelenmesi için kullandığımız adapter sınıfımız
class GalleryAdapter(private val context: Context, private var imageList: List<Uri>) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.galleryItem)
    }
    //
    @SuppressLint("NotifyDataSetChanged")
    fun updateImages(newImages: List<Uri>) {
        imageList = newImages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder , position: Int) {
        Glide.with(context).load(imageList[position]).into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}
