package com.example.track.data

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow

object TrackStore {
    val points = MutableStateFlow<List<TrackPoint>>(emptyList())
    val status = MutableStateFlow(TrackStat.IDLE)
    val currentLocation = MutableStateFlow<Location?>(null)
    val distanceMeters = MutableStateFlow(0f)
}