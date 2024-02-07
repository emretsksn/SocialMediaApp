package com.emretaskesen.tpost.ui.activity.images

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityCameraBinding
import com.emretaskesen.tpost.ui.fragment.images.GalleryFragment
import com.emretaskesen.tpost.util.ConstVal.Permissions.REQUEST_CODE
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    // Kullanılacak izinleri tanımla
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val permissions = arrayOf(
        Manifest.permission.CAMERA ,
        Manifest.permission.WRITE_EXTERNAL_STORAGE ,
        Manifest.permission.READ_EXTERNAL_STORAGE ,
        Manifest.permission.ACCESS_FINE_LOCATION ,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION ,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    lateinit var binding : ActivityCameraBinding
    private var imageCapture : ImageCapture? = null
    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService
    private lateinit var cameraProvider : ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var selectedImage : Uri
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // İzinleri kontrol et ve gerekirse iste
        checkAndRequestPermissions()
        initViewsAndFunctions()
    }

    @Suppress("DEPRECATION")
    private fun initViewsAndFunctions() {
        // Tüm izinler verilmişse kamera izinlerini kontrol et
        // Kamera izni yoksa iste
        if (allPermissionsGranted()) {
            startCamera()
        }
        else {
            ActivityCompat.requestPermissions(
                this , REQUIRED_PERMISSIONS , REQUEST_CODE_PERMISSIONS
            )
        }

        // Fotoğraf çekme butonu için tıklama dinleyicisini ayarla
        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        // Kamera değiştirme butonu için tıklama dinleyicisini ayarla
        binding.cameraChange.setOnClickListener {
            toggleCamera()
        }

        // Flaş butonu için tıklama dinleyicisini ayarla
        binding.cameraFlash.setOnClickListener {
            toggleFlash()
        }

        // Tamam butonu için tıklama dinleyicisini ayarla
        binding.completeButton.setOnClickListener {
            backIntent(selectedImage)
        }

        // Geri butonu için tıklama dinleyicisini ayarla
        binding.backPage.setOnClickListener {
            this.finish()
        }

        // Galeri seçimi için tıklama dinleyicisini ayarla
        binding.selectGallery.setOnClickListener {
            GalleryFragment().show(supportFragmentManager,"Galeri")
        }

        // Kamera işlemleri için bir executor başlat
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Önizlemeyi ayarla
        Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build().also {
            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }.also { preview = it }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val listPermissionsNeeded = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this , permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this , listPermissionsNeeded.toTypedArray() , REQUEST_CODE
            )
        }
    }

    private fun takePhoto() {
        // Kararlı bir referans alın
        // değiştirilebilir görüntü yakalama kullanım durumu
        val imageCapture = imageCapture ?: return

        // Resmi tutmak için zaman damgalı çıktı dosyası oluşturun
        val name = SimpleDateFormat(FILENAME_FORMAT , Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Dosya + meta verileri içeren çıktı seçenekleri nesnesi oluşturun
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Görüntü yakalama dinleyicisini ayarla,fotoğraf çekildikten sonra tetiklenen alınmış
        imageCapture.takePicture(
            outputOptions ,
            ContextCompat.getMainExecutor(this) ,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc : ImageCaptureException) {
                    // Hata durumunda gerekirse işlemler ekleyebilirsiniz
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val resultUri= output.savedUri
                    if (resultUri!=null){
                        selectedImage = resultUri
                        backIntent(resultUri)
                    }
                }
            })
    }

    private fun backIntent(selectedImage:Uri){
        // Seçilen resmi geri gönder
        val returnIntent = Intent().apply {
            putExtra("selectedImage" , selectedImage.toString())
        }
        setResult(Activity.RESULT_OK , returnIntent)
        finish()
    }

    @Suppress("DEPRECATION")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Kameraların yaşam döngüsünü yaşam döngüsü sahibine bağlamak için kullanılır
            cameraProvider = cameraProviderFuture.get()

            // Ön izleme
            imageCapture = ImageCapture.Builder().apply {
                // Bu örnekte flaşı otomatik olarak ayarlayabiliriz
                setFlashMode(FLASH_MODE_AUTO)
                // Görüntü yakalama modunu ayarla, öne kamera seçiliyse
                setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // Ayna görüntüsü olarak kaydetmek istiyorsanız
                if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    setTargetRotation(Surface.ROTATION_0)
                }
                setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
            }.build()

            // Arka kamerayı varsayılan olarak seç
            val cameraSelector = lensFacing

            try {
                // Yeniden bağlamadan önce kullanım durumlarının bağlantısını kaldırın
                cameraProvider.unbindAll()

                // Kullanım senaryolarını kameraya bağlayın
                cameraProvider.bindToLifecycle(
                    this , cameraSelector , preview , imageCapture
                )

            } catch (_ : Exception) {
                // Hata durumunda gerekirse işlemler ekleyebilirsiniz
            }

        } , ContextCompat.getMainExecutor(this))
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        // Kamerayı durdur
        cameraProvider.unbindAll()
        // Kamerayı yeni seçici ile yeniden başlat
        startCamera()
    }

    @SuppressLint("RestrictedApi" , "SwitchIntDef")
    private fun toggleFlash() {
        imageCapture?.let { capture ->
            capture.flashMode = when (capture.flashMode) {
                FLASH_MODE_AUTO -> FLASH_MODE_ON
                FLASH_MODE_ON -> FLASH_MODE_OFF
                FLASH_MODE_OFF -> FLASH_MODE_AUTO
                else -> capture.flashMode
            }
            when (capture.flashMode) {
                FLASH_MODE_OFF -> {
                    val flashOff: Drawable? = ContextCompat.getDrawable(this , R.drawable.ic_flash_off)
                    binding.cameraFlash.icon = flashOff
                }
                FLASH_MODE_ON -> {
                    val flashOn: Drawable? = ContextCompat.getDrawable(this , R.drawable.ic_flash_on)
                    binding.cameraFlash.icon = flashOn
                }
                FLASH_MODE_AUTO -> {
                    val flashOn: Drawable? = ContextCompat.getDrawable(this , R.drawable.ic_flash_auto)
                    binding.cameraFlash.icon = flashOn
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext , it) == PackageManager.PERMISSION_GRANTED
    }

    // Kamera izinlerini kontrol ediyoruz
    override fun onRequestPermissionsResult(
        requestCode : Int , permissions : Array<String> ,
        grantResults : IntArray ,
    ) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Eğer tüm izinler verilmişse, Kamera'yı başlat
            if (allPermissionsGranted()) {
                startCamera()
            }
            else {
                // İzin verilmediyse,
                // kullanıcıya bunu bildirmek için bir tost sunun
                // izinler verilmedi.
                Toast.makeText(this , "Permissions not granted by the user." , Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}