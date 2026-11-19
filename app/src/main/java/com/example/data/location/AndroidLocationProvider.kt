package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.GeoLocation
import com.example.domain.repository.LocationGateway
import com.example.domain.repository.LocationLookupResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android GPS location provider backed by Google Play Services [FusedLocationProviderClient]
 * with [LocationManager] fallback and locale-aware reverse geocoding via [Geocoder].
 */
class AndroidLocationProvider(
    private val context: Context,
) : LocationGateway {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    override fun hasLocationPermission(): Boolean {
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return hasFineLocationPermission() || coarseGranted
    }

    private fun isSystemLocationEnabled(): Boolean {
        val manager = locationManager ?: return true
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(languageCode: String): LocationLookupResult {
        if (!hasLocationPermission()) {
            return LocationLookupResult.PermissionMissing
        }
        if (!isSystemLocationEnabled()) {
            return LocationLookupResult.LocationDisabled
        }

        val androidLocation = withTimeoutOrNull(8_000L) {
            requestFreshLocation()
                ?: requestLastKnownFusedLocation()
                ?: requestLastKnownPlatformLocation()
        } ?: return LocationLookupResult.Unavailable("GPS fix unavailable")

        val resolved = reverseGeocodeLocationName(
            latitude = androidLocation.latitude,
            longitude = androidLocation.longitude,
            languageCode = languageCode,
        )
        val nearestStation = ImgwStationCatalog.findNearestCatalogStation(
            androidLocation.latitude,
            androidLocation.longitude,
        ).station

        val cityName = resolved?.first ?: nearestStation.name
        val admin1 = resolved?.second

        return LocationLookupResult.Success(
            GeoLocation(
                name = cityName,
                admin1 = admin1,
                latitude = androidLocation.latitude,
                longitude = androidLocation.longitude,
                isCurrentGps = true,
                terytVoivodeshipCode = ImgwStationCatalog.resolveVoivodeshipTeryt(
                    explicitTeryt = null,
                    admin1 = admin1,
                    fallbackStation = nearestStation,
                ),
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            continuation.invokeOnCancellation { cts.cancel() }

            val priority = if (hasFineLocationPermission()) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            fusedClient
                .getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastKnownFusedLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }

    @SuppressLint("MissingPermission")
    private fun requestLastKnownPlatformLocation(): Location? {
        val manager = locationManager ?: return null
        val providers = buildList {
            if (hasFineLocationPermission()) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    override suspend fun reverseGeocodeLocationName(
        latitude: Double,
        longitude: Double,
        languageCode: String,
    ): Pair<String, String?>? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val locale = Locale.forLanguageTag(languageCode)
            val geocoder = Geocoder(context, locale)

            val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    runCatching {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }
                    }.onFailure {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
            }

            if (address == null) return@withContext null
            val city = address.locality
                ?: address.subAdminArea
                ?: address.featureName
                ?: return@withContext null
            Pair(city, address.adminArea)
        }
}
