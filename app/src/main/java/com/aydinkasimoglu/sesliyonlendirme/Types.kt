package com.aydinkasimoglu.sesliyonlendirme

import kotlinx.serialization.Serializable

@Serializable
data class Data(val routes: List<Route> = listOf())

@Serializable
data class Route(val legs: List<Leg> = listOf())

@Serializable
data class Leg(val steps: List<Step> = listOf())

@Serializable
data class Step(val navigationInstruction: NavigationInstruction? = null, val localizedValues: LocalizedValues)

@Serializable
data class NavigationInstruction(val maneuver: String, val instructions: String)

@Serializable
data class LocalizedValues(val distance: Value, val staticDuration: Value)

@Serializable
data class Value(val text: String)

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
//========
@Serializable
data class LocationData(val results: List<Result> = listOf())

@Serializable
data class Result(val geometry: Geometry)

@Serializable
data class Geometry(val location: GeometryLocation)

@Serializable
data class GeometryLocation(val lat: Double, val lng: Double)