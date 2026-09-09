package com.example.track.vm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.track.data.TrackStat

class TrackViewModel(app: Application) : AndroidViewModel(app) {
    private val locationManager =
        app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var status by mutableStateOf(TrackStat.IDLE); private set
    var pointCount by mutableStateOf(0);private set
    var currentLocation by mutableStateOf<Location?>(null); private set
    var gpsError by mutableStateOf<String?>(null); private set

    private val listener = LocationListener{ loc ->
        currentLocation=loc
        if (status == TrackStat.RECORDING){
            pointCount++
        }
    }
    @SuppressLint("Missing permission")
    fun startLocationUpdates(){
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            gpsError = "GPS выключен. Включите геолокацию в настройках устройства."
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000L,
            1f,
            listener,
            Looper.getMainLooper()
        )
    }
    fun toggleRecording(){
        status = if (status == TrackStat.RECORDING) TrackStat.STOPPED
        else TrackStat.RECORDING
    }
    fun consumeGpsError(){gpsError = null}
    override fun onCleared(){
        locationManager.removeUpdates(listener)
    }
}