package com.example.track.data

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.collections.plusAssign

class TrackRecorderService : Service() {

    private lateinit var locationManager: LocationManager

    private val listener = LocationListener { loc ->
        TrackStore.currentLocation.value = loc
        if (TrackStore.status.value == TrackStat.RECORDING) {
            TrackStore.points.value.lastOrNull()?.let { prev ->
                val r = FloatArray(1)
                Location.distanceBetween(prev.lat, prev.lon, loc.latitude, loc.longitude, r)
                TrackStore.distanceMeters.value += r[0]
            }
            TrackStore.points.value += TrackPoint(loc.time, loc.latitude, loc.longitude)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val channel = NotificationChannel(
            "track", "Запись трека", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // сервис стартует только после granted
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif: Notification = NotificationCompat.Builder(this, "track")
            .setContentTitle("TrackMe")
            .setContentText("Идёт запись трека...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notif)
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 2000L, 1f, listener, Looper.getMainLooper()
        )
        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(listener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}