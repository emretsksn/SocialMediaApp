package com.emretaskesen.tpost.ui.activity.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emretaskesen.tpost.BuildConfig
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.databinding.ActivityMapsBinding
import com.emretaskesen.tpost.util.ConstVal.Permissions.REQUEST_CODE
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import timber.log.Timber
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    @RequiresApi(Build.VERSION_CODES.Q)
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var selectedLocation: LatLng? = null
    private var locationName: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAndRequestPermissions()
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {

        binding.userprofileToolbar.setNavigationIcon(R.drawable.ic_arrow_left)
        binding.userprofileToolbar.setNavigationOnClickListener {
            this.finish()
        }

        // Google Places API'yi başlat
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        // Otomatik tamamlama için PlacesFragment'ı ayarla
        val placesFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        placesFragment.setHint(getString(R.string.searh_location))
        placesFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG))
        placesFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedLocation = place.latLng!!
                locationName = place.name!!
                binding.selectedLocation.text = locationName
                mMap.clear()
                mMap.addMarker(
                    MarkerOptions()
                        .position(selectedLocation!!)
                        .title(place.name)
                        .draggable(true)
                )
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        selectedLocation!!,
                        12f
                    )
                )
            }

            override fun onError(status: Status) {
                Timber.tag("PlacesError")
                    .e("Yer seçimi başarısız oldu: ${status.statusMessage} ${status.statusCode}")
                Toast.makeText(applicationContext, R.string.places_status_error, Toast.LENGTH_LONG)
                    .show()
            }
        })

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.sendLocation.isActivated = selectedLocation!=null
        binding.sendLocation.setOnClickListener {
            if (selectedLocation!=null){
                // Sonuçları intent ile geri gönder
                val returnIntent = Intent().apply {
                    putExtra("latLang", selectedLocation)
                    putExtra("locationName", locationName)
                }
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }else{
                Toast.makeText(applicationContext,"Konum Seçiniz",Toast.LENGTH_LONG).show()
            }

        }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onMapReady(googleMap: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // İzin yoksa izinleri kontrol et ve iste
            checkAndRequestPermissions()
        }
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.run {
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = true
            isCompassEnabled = true
            isRotateGesturesEnabled = true
            isZoomControlsEnabled = true
            isScrollGesturesEnabledDuringRotateOrZoom = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isIndoorLevelPickerEnabled = true
        }
        mMap.setOnMapClickListener { latLng ->
            locationName = null
            mMap.clear()
            // Tıklanan konumu kullanarak adresi al
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = addresses?.get(0)?.getAddressLine(0) // Yeri temsil eden tam adres alınıyor.
            locationName = "$address"
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(locationName)
                    .draggable(true)
            )
            selectedLocation = LatLng(latLng.latitude, latLng.longitude)
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    selectedLocation!!,
                    12f
                )
            )
            binding.selectedLocation.text = locationName
        }

        // Son konumu al ve yerleri ara
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let { latLng ->
                val placesClient =
                    Places.createClient(this@MapsActivity) // Yerleri aramak için PlacesClient oluştur
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
                val request = FindCurrentPlaceRequest.newInstance(placeFields)

                placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                    for (placeLikelihood in response.placeLikelihoods) {
                        val place = placeLikelihood.place
                        val placeId = place.id
                        if (placeId != null) {
                            val placeRequest = FetchPlaceRequest.newInstance(placeId, placeFields)
                            placesClient.fetchPlace(placeRequest)
                                .addOnSuccessListener { fetchPlaceResponse ->
                                    mMap.clear()
                                    val fetchedPlace =
                                        fetchPlaceResponse.place
                                    selectedLocation = LatLng(latLng.latitude, latLng.longitude)
                                    locationName = fetchedPlace.name ?: fetchedPlace.address ?: ""
                                    binding.selectedLocation.text = locationName
                                    mMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            selectedLocation!!,
                                            12f
                                        )
                                    )
                                    mMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(latLng.latitude, latLng.longitude))
                                            .title(locationName)
                                            .draggable(true)
                                    )
                                }.addOnFailureListener { exception ->
                                    if (exception is ApiException) {
                                        Timber.tag("PlacesError")
                                            .e(
                                                "Yer seçimi başarısız oldu: ${exception.statusMessage} ${exception.statusCode}"
                                            )
                                    }
                                }
                            break
                        }
                    }
                }.addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        Timber.tag("PlacesError")
                            .e(
                                "Yer seçimi başarısız oldu: ${exception.statusMessage} ${exception.statusCode}"
                            )
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        // İzinleri kontrol et ve iste
        val listPermissionsNeeded = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // İzin isteğine cevapları kontrol et
        if (requestCode == 100) when {
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                return
            }
        }
    }
}
