package com.dreamtravel.data.remote

import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import com.dreamtravel.data.model.SearchResult
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlaceSearchService @Inject constructor(
    private val geocodeSearch: GeocodeSearch
) {

    suspend fun searchCity(name: String): SearchResult? {
        return suspendCancellableCoroutine { continuation ->
            val query = GeocodeQuery(name, null)
            val listener = object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(result: GeocodeResult?, errorCode: Int) {
                    if (continuation.isCancelled) return
                    if (errorCode == com.amap.api.services.core.AMapException.CODE_AMAP_SUCCESS
                        && result != null
                        && !result.geocodeAddressList.isNullOrEmpty()
                    ) {
                        val address = result.geocodeAddressList[0]
                        val latLon: LatLonPoint? = address.latLonPoint
                        continuation.resume(
                            SearchResult(
                                name = address.formatAddress ?: name,
                                cityCode = address.adcode,
                                latitude = latLon?.latitude ?: 0.0,
                                longitude = latLon?.longitude ?: 0.0
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }

                override fun onRegeocodeSearched(result: RegeocodeResult?, errorCode: Int) {
                    // Not used for forward geocoding
                }
            }
            geocodeSearch.setOnGeocodeSearchListener(listener)
            geocodeSearch.getFromLocationNameAsyn(query)

            continuation.invokeOnCancellation {
                // GeocodeSearch doesn't have a cancel method, but we can detach listener
                geocodeSearch.setOnGeocodeSearchListener(null)
            }
        }
    }
}
