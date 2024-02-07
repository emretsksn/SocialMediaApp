package com.emretaskesen.tpost.model
import android.net.Uri

data class CropSettings(
    val sourceUri: Uri ,
    val destinationUri: Uri ,
    val aspectRatioX: Float? = null ,
    val aspectRatioY: Float? = null
)