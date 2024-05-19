package com.aydinkasimoglu.sesliyonlendirme

import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aydinkasimoglu.sesliyonlendirme.ui.theme.SesliYonlendirmeTheme
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    val voiceToText by lazy {
        VoiceToText(application)
    }

    private lateinit var textToSpeech: TextToSpeech

    fun speakInstruction(instruction: String) {
        textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.forLanguageTag("tr-TR")
                textToSpeech.setSpeechRate(1.3f)
            }
        }

        setContent {
            var currentLocation by rememberSaveable {
                mutableStateOf(0.0 to 0.0)
            }

            var directionsData by rememberSaveable(stateSaver = directionsSaver) {
                mutableStateOf(null)
            }

            val scope = rememberCoroutineScope()

            val voiceState by voiceToText.state.collectAsState()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)

                    p0.lastLocation.let { location ->
                        if (location != null) {
                            currentLocation = location.latitude to location.longitude
                        }
                    }

                    if (voiceState.spokenText.isNotEmpty()) {
                        // TODO: Get destination location from the spoken text
                        val destinationLocation = 40.907186 to 29.170051

                        scope.launch(Dispatchers.IO) {
                            directionsData = getDirectionsFrom(currentLocation to destinationLocation)
                        }
                    }
                }
            }

            SesliYonlendirmeTheme {
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

                            Spacer(modifier = Modifier.height(40.dp))

                            DirectionsScreen(
                                currentLocation = currentLocation,
                                data = directionsData
                            )
                        }
                    }
                }
            }
        }
    }
}