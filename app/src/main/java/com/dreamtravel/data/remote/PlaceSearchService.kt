package com.dreamtravel.data.remote

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import com.dreamtravel.data.model.SearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlaceSearchService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun searchCity(name: String): SearchResult? {
        // 每次请求创建独立实例，避免并发调用 listener 覆盖竞态
        val localSearch = GeocodeSearch(context)
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
            localSearch.setOnGeocodeSearchListener(listener)
            localSearch.getFromLocationNameAsyn(query)

            continuation.invokeOnCancellation {
                localSearch.setOnGeocodeSearchListener(null)
            }
        }
    }
}
