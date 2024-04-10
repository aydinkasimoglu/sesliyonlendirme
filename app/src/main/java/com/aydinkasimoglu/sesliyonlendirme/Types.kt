package com.aydinkasimoglu.sesliyonlendirme

import kotlinx.serialization.Serializable

@Serializable
data class Data(val routes: List<Route> = listOf())

@Serializable
data class Route(val legs: List<Leg> = listOf())

@Serializable
data class Leg(val steps: List<Step> = listOf())

@Serializable
data class Step(val navigationInstruction: NavigationInstruction? = null)

@Serializable
data class NavigationInstruction(val maneuver: String, val instructions: String)

@Serializable
data class RequestBody(val origin: Origin, val destination: Destination, val travelMode: String, val languageCode: String, val units: String)

@Serializable
data class Origin(val location: RequestLocation)

@Serializable
data class Destination(val location: RequestLocation)

@Serializable
data class RequestLocation(val latLng: LatLng)

@Serializable
data class LatLng(val latitude: Double, val longitude: Double)