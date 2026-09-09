package com.example.track.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color.WHITE
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.track.data.TrackStat
import com.example.track.vm.TrackViewModel
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

@Composable
fun TrackScreen(viewModel: TrackViewModel){
    val osmDe = org.osmdroid.tileprovider.tilesource.XYTileSource(
        "OSM-DE",
        0, 19, 256, ".png",
        arrayOf("https://tile.openstreetmap.de/")
    )
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    var permissionGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                        PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        results ->
        permissionGranted = results.values.any{it}
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(requiredPermissions)
    }
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) viewModel.startLocationUpdates()
    }
    val gpsError = viewModel.gpsError
    LaunchedEffect(gpsError) {
        gpsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeGpsError()
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // карта
            AndroidView(
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        clipChildren = true
                        addView(
                            MapView(ctx).apply {
                                setTileSource(osmDe)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                                controller.setZoom(6.0)

                                // Маркер текущего положения
                                val myLocation = MyLocationNewOverlay(this).apply {
                                    setPersonIcon(createLocationIcon())
                                    setDirectionIcon(createLocationIcon())
                                    enableMyLocation()
                                    enableFollowLocation() // карта само центрируется на вас
                                }
                                overlays.add(myLocation)
                            },
                            android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clipToBounds()
            )
            // панель
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val statusText: String
                val statusColor: Color
                when (viewModel.status) {
                    TrackStat.IDLE -> {
                        statusText = "Ожидание"
                        statusColor = Color.Gray
                    }
                    TrackStat.RECORDING -> {
                        statusText = "Идёт запись..."
                        statusColor = Color(0xFFD32F2F)
                    }
                    TrackStat.STOPPED -> {
                        statusText = "Запись остановлена. Точек: ${viewModel.pointCount}"
                        statusColor = Color(0xFF388E3C)
                    }
                }
                Text(statusText, color = statusColor, style = MaterialTheme.typography.titleMedium)

                // Счётчик (отдельно, чтобы был всегда)
                Text("Точек: ${viewModel.pointCount}")

                // Кнопка с двумя состояниями
                val recording = viewModel.status == TrackStat.RECORDING
                Button(
                    onClick = { viewModel.toggleRecording() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recording) Color(0xFFD32F2F)
                        else Color(0xFF388E3C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (recording) "Остановить запись" else "Начать запись")
                }
            }
        }
    }

}
private fun createLocationIcon(): Bitmap {
    val size = 96
    val bmp = createBitmap(size, size)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = "#2196F3".toColorInt() // синий круг
    canvas.drawCircle(size / 2f, size / 2f, size / 2.6f, paint)
    paint.color = WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)
    return bmp
}