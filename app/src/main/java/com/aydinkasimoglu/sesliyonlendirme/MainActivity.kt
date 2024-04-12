package com.aydinkasimoglu.sesliyonlendirme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aydinkasimoglu.sesliyonlendirme.ui.theme.SesliYonlendirmeTheme
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val voiceToText by lazy {
        VoiceToText(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            var currentLocation by remember {
                mutableStateOf(Pair(0.0, 0.0))
            }

            var directionsData by remember {
                mutableStateOf<Step?>(null)
            }

            val scope = rememberCoroutineScope()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)

                    for (location in p0.locations) {
                        currentLocation = Pair(location.latitude, location.longitude)
                    }

                    scope.launch(Dispatchers.IO) {
                        directionsData = getDirectionsData(currentLocation)
                    }
                }
            }

            SesliYonlendirmeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Column {
                            Speech(voiceToText = voiceToText)
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            DirectionsScreen(currentLocation = currentLocation, data = directionsData)
                        }
                    }
                }
            }
        }
    }
}
