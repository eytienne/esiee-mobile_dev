package com.example.exo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.CompassListener
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var headingTV: TextView
    private val locationO = ObservableField<Location>()

    companion object {
        const val SOURCE_ID = "location-source";
        const val IMAGE_ID = "location-image";
        const val LAYER_ID = "location-layer";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationO.one {
            findViewById<TextView>(R.id.latitude_TV).text = "${it?.longitude}"
            findViewById<TextView>(R.id.longitude_TV).text = "${it?.latitude}"
        }

        val mapTilerKey = BuildConfig.maptiler_key
        val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=${mapTilerKey}";

        // Get the MapBox context
        Mapbox.getInstance(this, null)

        // Set the map view layout
        setContentView(R.layout.activity_main)

        headingTV = findViewById(R.id.heading_TV)

        // Create map view
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            // Set the style after mapView was loaded
            map.setStyle(styleUrl) {
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                val callback = locationO.one {
                    setViewCenter(map, it)
                }
                val location = locationO.get()
                if (location != null) {
                    setViewCenter(map, location)
                    locationO.off(callback)
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PackageManager.PERMISSION_GRANTED
            )
        } else {
            initLocation();
        }
    }

    private fun setViewCenter(map: MapboxMap, location: Location?) {
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(location))
            .zoom(10.0)
            .build()

        if (location == null) return
        map.getStyle {
            // location marker
            val icon = BitmapFactory.decodeResource(resources, R.drawable.location_pin)
            it.addImage(IMAGE_ID, icon)

            val featureCollection = FeatureCollection.fromFeature(
                Feature.fromGeometry(
                    Point.fromLngLat(location.longitude, location.latitude)
                )
            )
            val geoJsonSource = GeoJsonSource(SOURCE_ID, featureCollection)
            it.addSource(geoJsonSource)

            val symbolLayer = SymbolLayer(LAYER_ID, SOURCE_ID)
            symbolLayer.setProperties(
                PropertyFactory.iconImage(IMAGE_ID),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconRotate(180f),
                PropertyFactory.iconOpacity(0.65f),
                PropertyFactory.iconSize(0.04f),
            )

            it.addLayer(symbolLayer)

            // heading
            map.locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.Builder(
                    this,
                    it
                ).build()
            )
            map.locationComponent.compassEngine?.addCompassListener(object : CompassListener {
                @SuppressLint("SetTextI18n")
                override fun onCompassChanged(userHeading: Float) {
                    headingTV.text = "$userHeading°"
                    map.cameraPosition =
                        CameraPosition.Builder(map.cameraPosition).bearing(userHeading.toDouble())
                            .build();
                }

                override fun onCompassAccuracyChange(compassStatus: Int) {

                }
            })

        }

    }

    @SuppressLint("MissingPermission")
    fun initLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                locationO.set(location)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        initLocation();
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}

// JQuery-like methods for ObservableField

private fun <T> ObservableField<T>.on(function: (value: T?) -> Unit): Observable.OnPropertyChangedCallback {
    val oThis = this
    val callback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val value = oThis.get()
            function(value);
        }
    }
    this.addOnPropertyChangedCallback(callback);
    return callback
}

private fun <T> ObservableField<T>.one(function: (value: T?) -> Unit): Observable.OnPropertyChangedCallback {
    val callback = this.on(function)
    this.on {
        this.off(callback)
    }
    return callback
}

private fun <T> ObservableField<T>.off(callback: Observable.OnPropertyChangedCallback) {
    this.removeOnPropertyChangedCallback(callback)
}