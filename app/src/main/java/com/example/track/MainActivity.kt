package com.example.track

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import com.example.track.view.TrackScreen
import com.example.track.vm.TrackViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val viewModel: TrackViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )

        enableEdgeToEdge()
        setContent {
            TrackScreen(
                viewModel = viewModel
            )
        }
    }
}


