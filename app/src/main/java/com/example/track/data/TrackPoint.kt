package com.example.track.data

import org.osmdroid.util.GeoPoint

data class TrackPoint(
    val timeMs: Long,
    val lat: Double,
    val lon: Double
) {
    fun toGeoPoint() = GeoPoint(lat, lon)
}