package com.example.track.vm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.track.data.TrackPoint
import com.example.track.data.TrackStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackViewModel(app: Application) : AndroidViewModel(app) {
    private val locationManager =
        app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var status by mutableStateOf(TrackStat.IDLE); private set

    var points by mutableStateOf<List<TrackPoint>>(emptyList()); private set
    var exportMessage by mutableStateOf<String?>(null); private set
    var currentLocation by mutableStateOf<Location?>(null); private set
    var gpsError by mutableStateOf<String?>(null); private set

    private val listener = LocationListener { loc ->
        currentLocation = loc
        if (status == TrackStat.RECORDING) {
            points = points + TrackPoint(loc.time, loc.latitude, loc.longitude)
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
    fun toggleRecording() {
        when (status) {
            TrackStat.RECORDING -> {
                status = TrackStat.STOPPED
                exportTrack()          // файл формируется сразу после остановки
            }
            else -> {
                points = emptyList()
                status = TrackStat.RECORDING
                // стартовая точка
                currentLocation?.let { loc ->
                    points = points + TrackPoint(loc.time, loc.latitude, loc.longitude)
                }
            }
        }
    }
    fun consumeGpsError() { gpsError = null }
    fun consumeExportMessage() { exportMessage = null }
    override fun onCleared(){
        locationManager.removeUpdates(listener)
    }
    fun exportTrack() {
        if (points.isEmpty()) {
            exportMessage = "Нет точек для сохранения"
            return
        }

        val fileName = "track_${
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        }.csv"

        //время, широта, долгота
        val csv = buildString {
            appendLine("time,lat,lon")
            points.forEach { p ->
                appendLine("${p.timeMs},${p.lat},${p.lon}")
            }
        }

        // MediaStore
        val resolver = getApplication<Application>().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { stream ->
                stream.write(csv.toByteArray())
            }
        }

        exportMessage = "Сохранено: ${Environment.DIRECTORY_DOWNLOADS}/$fileName"
    }
}