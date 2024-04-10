package com.aydinkasimoglu.sesliyonlendirme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.aydinkasimoglu.sesliyonlendirme.ui.theme.SesliYonlendirmeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json


class MainActivity : ComponentActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

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
                        directionsData = makeRequest(currentLocation)
                    }
                }
            }

            SesliYonlendirmeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DirectionsScreen(currentLocation = currentLocation, data = directionsData)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun makeRequest(origin: Pair<Double, Double>): Step? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }

            install(HttpTimeout) {
                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
        }

        val response: HttpResponse = client.post("https://routes.googleapis.com/directions/v2:computeRoutes") {
            url {
                headers.append("Content-Type", "application/json")
                headers.append("X-Goog-Api-Key", "AIzaSyAlsV41_jlFTFhzWwTLy7TpeioSuggP7Q0")
                headers.append("X-Goog-FieldMask", "routes.legs")
            }
            setBody(RequestBody(
                origin = Origin(RequestLocation(LatLng(origin.first, origin.second))),
                destination = Destination(RequestLocation(LatLng(40.82408137774701, 29.920700725944666))),
                travelMode = "WALK",
                languageCode = "tr-TR",
                units = "METRIC"
            ))
        }

        client.close()

        return if (response.status.value in 200..299) {
            val data = response.body<Data>()
            data.routes[0].legs[0].steps[0]
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()

            locationClient.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    @Composable
    private fun DirectionsScreen(currentLocation: Pair<Double, Double>, data: Step?) {
        val launcherMultiplePermissions =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) {
                val areGranted = it.values.reduce { acc, next -> acc && next }

                if (areGranted) {
                    startLocationUpdates()
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
                }
                else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
        }

        LaunchedEffect (key1 = permissions) {
            if (permissions.all {
                    ActivityCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
                }) {
                startLocationUpdates()
            }
            else {
                launcherMultiplePermissions.launch(permissions)
            }
        }

        Box (
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column (
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ElevatedCard (
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Text (
                        text =
                            if (currentLocation == Pair(0.0, 0.0))
                                "Konumunuz yükleniyor"
                            else
                                "Konumunuz: ${currentLocation.first}/${currentLocation.second}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                }
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    data?.also {
                        it.navigationInstruction?.let { navigationInstruction ->
                            Text(text = navigationInstruction.instructions)
                        }
                    } ?: CircularProgressIndicator (modifier = Modifier.width(45.dp))
                }
            }
        }
    }
}
