package com.dreamtravel.data.model

enum class RegionLevel {
    PROVINCE,
    CITY,
    DISTRICT
}

enum class RegionPathMode {
    CITY_OPTIONAL_DISTRICT,
    DIRECT_DISTRICT
}

data class RegionNode(
    val code: String,
    val name: String,
    val level: RegionLevel,
    val parentCode: String? = null,
    val children: List<RegionNode> = emptyList()
)

data class SelectedRegion(
    val provinceCode: String,
    val provinceName: String,
    val cityCode: String,
    val cityName: String,
    val districtCode: String?,
    val districtName: String?,
    val formattedAddress: String
)
