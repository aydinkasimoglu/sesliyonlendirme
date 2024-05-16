package com.aydinkasimoglu.sesliyonlendirme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

lateinit var locationClient: FusedLocationProviderClient
lateinit var locationCallback: LocationCallback
val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * This is a Saver object for saving and restoring Step objects.
 * It is used to persist the Step object across configuration changes such as screen rotations.
 */
val directionsSaver = Saver<Step?, String>(
    save = {
        it?.let {
            Json.encodeToString(Step.serializer(), it)
        } ?: ""
    },
    restore = {
        if (it.isNotEmpty()) {
            Json.decodeFromString(Step.serializer(), it)
        } else {
            null
        }
    }
)

/**
 * Get directions data from Google Maps Routes API
 *
 * @param origin The origin location as a pair of latitude and longitude
 * @return The first step of the route
 */
@OptIn(ExperimentalSerializationApi::class)
suspend fun getDirectionsData(origin: Pair<Double, Double>): Step? {
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
fun startLocationUpdates() {
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

/**
 * Screen for displaying the location of the user and the navigation instruction
 *
 * @param currentLocation The current location as a pair of latitude and longitude
 * @param data The step data
 */
@Composable
fun DirectionsScreen(currentLocation: Pair<Double, Double>, data: Step?) {
    val context = LocalContext.current

    val launcherMultiplePermissions =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) {
            val areGranted = it.values.reduce { acc, next -> acc && next }

            if (areGranted) {
                startLocationUpdates()
                Toast.makeText(context, "Permission granted", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
            }
        }

    LaunchedEffect (key1 = launcherMultiplePermissions) {
        if (permissions.all {
                ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startLocationUpdates()
        }
        else {
            launcherMultiplePermissions.launch(permissions)
        }
    }

    Column (
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ElevatedCard (
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Text (
                text = if (currentLocation == Pair(0.0, 0.0))
                    "Konumunuz yükleniyor"
                else
                    "Konumunuz: ${currentLocation.first}/${currentLocation.second}",
                style = MaterialTheme.typography.bodyMedium,
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
                    Text(
                        text = navigationInstruction.instructions,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } ?: CircularProgressIndicator (modifier = Modifier.width(45.dp))
        }
    }
}