package com.dreamtravel.domain

import com.dreamtravel.data.model.Place
import javax.inject.Inject

/**
 * 高德地图城市搜索 + 逆地理编码的封装
 * 在 UI 层调用高德 SDK，此 UseCase 作为数据转换层
 */
class CityDetectionUseCase @Inject constructor() {

    /**
     * 标准化城市名：去除"市""州""地区""自治州"等后缀
     */
    fun normalizeCityName(name: String): String {
        return name
            .replace("市", "")
            .replace("州", "")
            .replace("地区", "")
            .replace("自治州", "")
            .replace("白族自治州", "")
            .trim()
    }

    /**
     * 判断逆地理编码返回的城市名是否匹配梦想城市
     */
    fun isCityMatch(geocodeCityName: String, dreamPlace: Place): Boolean {
        // 1. cityCode 精确匹配（最可靠）
        if (!dreamPlace.cityCode.isNullOrBlank() && geocodeCityName.contains(dreamPlace.cityCode)) {
            return true
        }

        // 2. 标准化后字符串匹配
        val normalizedGeocode = normalizeCityName(geocodeCityName)
        val normalizedDream = normalizeCityName(dreamPlace.name)

        // 3. 双向包含检查（处理"大理市" vs "大理白族自治州" 这种情况）
        return normalizedGeocode.contains(normalizedDream) ||
                normalizedDream.contains(normalizedGeocode)
    }
}
