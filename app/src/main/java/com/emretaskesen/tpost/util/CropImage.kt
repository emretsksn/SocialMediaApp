package com.emretaskesen.tpost.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.emretaskesen.tpost.model.CropSettings
import com.yalantis.ucrop.UCrop

class CropImage : ActivityResultContract<CropSettings , Uri?>() {

    override fun createIntent(context: Context, input: CropSettings): Intent {
        // UCrop ayarlarını başlat
        val uCrop = UCrop.of(input.sourceUri, input.destinationUri)

        // Aspect ratio ayarlarını uygula
        input.aspectRatioX?.let { x ->
            input.aspectRatioY?.let { y ->
                uCrop.withAspectRatio(x, y)
            }
        }
        // Ekstra isteğe bağlı ayarlar eklenebilir buraya
        // Örneğin: uCrop.withMaxResultSize(width, height)

        // Intent'i döndür
        return uCrop.getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return UCrop.getOutput(intent)
    }
}