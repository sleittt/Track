package com.example.track.vm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.track.data.TrackPoint
import com.example.track.data.TrackRecorderService
import com.example.track.data.TrackStat
import com.example.track.data.TrackStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(DelicateCoroutinesApi::class)
class TrackViewModel(app: Application) : AndroidViewModel(app) {
    private val locationManager =
        app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var status by mutableStateOf(TrackStat.IDLE); private set

    var points by mutableStateOf<List<TrackPoint>>(emptyList()); private set
    var exportMessage by mutableStateOf<String?>(null); private set
    var currentLocation by mutableStateOf<Location?>(null); private set
    var gpsError by mutableStateOf<String?>(null); private set

    var distanceMeters by mutableStateOf(0f); private set
    var importedPoints by mutableStateOf<List<TrackPoint>>(emptyList()); private set
    var importMessage by mutableStateOf<String?>(null); private set

    private val listener = LocationListener { loc ->
        currentLocation = loc
        if (status == TrackStat.RECORDING) {
            points.lastOrNull()?.let { prev ->
                val result = FloatArray(1)
                Location.distanceBetween(prev.lat, prev.lon, loc.latitude, loc.longitude, result)
                distanceMeters += result[0]
            }
            points = points + TrackPoint(loc.time, loc.latitude, loc.longitude)
        }
    }
    init {
        GlobalScope.launch {
            TrackStore.points.collect { points = it }
        }
        GlobalScope.launch { TrackStore.status.collect { status = it } }
        GlobalScope.launch { TrackStore.currentLocation.collect { currentLocation = it } }
        GlobalScope.launch { TrackStore.distanceMeters.collect { distanceMeters = it } }
        GlobalScope.launch { TrackStore.gpsError.collect { gpsError = it } }
    }
    fun toggleRecording() {
        val app = getApplication<Application>()
        when (TrackStore.status.value) {
            TrackStat.RECORDING -> {
                TrackStore.status.value = TrackStat.STOPPED
                app.stopService(Intent(app, TrackRecorderService::class.java))
                exportTrack()
            }
            else -> {
                TrackStore.points.value = emptyList()
                TrackStore.distanceMeters.value = 0f
                TrackStore.status.value = TrackStat.RECORDING
                app.startForegroundService(Intent(app, TrackRecorderService::class.java))
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
    fun importTrack(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text.isNullOrBlank()) {
            importMessage = "Не удалось прочитать файл"
            return
        }
        val parsed = text.lineSequence()
            .drop(1) // пропускаем шапку "time,lat,lon"
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val p = line.split(",")
                if (p.size < 3) return@mapNotNull null
                val t = p[0].trim().toLongOrNull()
                val lat = p[1].trim().toDoubleOrNull()
                val lon = p[2].trim().toDoubleOrNull()
                if (t != null && lat != null && lon != null) TrackPoint(t, lat, lon) else null
            }
            .toList()

        if (parsed.isEmpty()) {
            importMessage = "В файле нет валидных точек"
        } else {
            importedPoints = parsed
            importMessage = "Загружено точек: ${parsed.size}"
        }
    }

    fun consumeImportMessage() { importMessage = null }
}