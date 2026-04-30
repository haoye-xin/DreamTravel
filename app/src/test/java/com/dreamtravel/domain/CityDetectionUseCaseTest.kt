package com.dreamtravel.domain

import com.dreamtravel.data.model.Place
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CityDetectionUseCaseTest {

    private lateinit var useCase: CityDetectionUseCase

    @Before
    fun setUp() {
        useCase = CityDetectionUseCase()
    }

    // ─── normalizeCityName ────────────────────────────────────

    @Test
    fun `normalizeCityName removes 市 suffix`() {
        assertEquals("北京", useCase.normalizeCityName("北京市"))
        assertEquals("上海", useCase.normalizeCityName("上海市"))
        assertEquals("成都", useCase.normalizeCityName("成都市"))
    }

    @Test
    fun `normalizeCityName removes 州 suffix`() {
        assertEquals("杭州", useCase.normalizeCityName("杭州市"))
        assertEquals("广州", useCase.normalizeCityName("广州市"))
    }

    @Test
    fun `normalizeCityName removes 自治州 suffix`() {
        assertEquals("大理白", useCase.normalizeCityName("大理白族自治州"))
        assertEquals("恩施土家族苗", useCase.normalizeCityName("恩施土家族苗族自治州"))
    }

    @Test
    fun `normalizeCityName removes 地区 suffix`() {
        assertEquals("昌都", useCase.normalizeCityName("昌都地区"))
    }

    @Test
    fun `normalizeCityName no change for plain names`() {
        assertEquals("杭州", useCase.normalizeCityName("杭州"))
        assertEquals("大理", useCase.normalizeCityName("大理"))
    }

    @Test
    fun `normalizeCityName handles empty string`() {
        assertEquals("", useCase.normalizeCityName(""))
    }

    @Test
    fun `normalizeCityName handles whitespace`() {
        assertEquals("北京", useCase.normalizeCityName(" 北京市 "))
    }

    @Test
    fun `normalizeCityName removes multiple suffixes`() {
        // "白族自治州" is removed before "州" due to order of replace calls
        assertEquals("大理", useCase.normalizeCityName("大理白族自治州"))
    }

    // ─── isCityMatch ─────────────────────────────────────────

    @Test
    fun `isCityMatch exact match via normalize`() {
        val place = Place(
            id = "1", name = "大理", cityCode = null,
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertTrue(useCase.isCityMatch("大理市", place))
        assertTrue(useCase.isCityMatch("大理", place))
    }

    @Test
    fun `isCityMatch cityCode exact match`() {
        val place = Place(
            id = "1", name = "大理", cityCode = "大理",
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertTrue(useCase.isCityMatch("大理白族自治州", place))
    }

    @Test
    fun `isCityMatch cityCode in geocode name`() {
        val place = Place(
            id = "1", name = "成都", cityCode = "成都",
            latitude = 30.5, longitude = 104.0,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertTrue(useCase.isCityMatch("四川省成都市", place))
    }

    @Test
    fun `isCityMatch no match different city`() {
        val place = Place(
            id = "1", name = "大理", cityCode = null,
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertFalse(useCase.isCityMatch("昆明市", place))
    }

    @Test
    fun `isCityMatch no match cityCode mismatch`() {
        val place = Place(
            id = "1", name = "大理", cityCode = "大理",
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertFalse(useCase.isCityMatch("昆明市", place))
    }

    @Test
    fun `isCityMatch substring match in geocode`() {
        val place = Place(
            id = "1", name = "大理", cityCode = null,
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertTrue(useCase.isCityMatch("云南省大理市", place))
    }

    @Test
    fun `isCityMatch bidirectional contains`() {
        val place = Place(
            id = "1", name = "大理白族自治州", cityCode = null,
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        // After normalization: "大理白" contains "大理" (dream name normalized)
        assertTrue(useCase.isCityMatch("大理", place))
    }

    @Test
    fun `isCityMatch null cityCode fallback to name match`() {
        val place = Place(
            id = "1", name = "上海", cityCode = null,
            latitude = 31.2, longitude = 121.5,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertTrue(useCase.isCityMatch("上海市", place))
    }

    @Test
    fun `isCityMatch blank geocode returns false`() {
        val place = Place(
            id = "1", name = "大理", cityCode = null,
            latitude = 25.6, longitude = 100.2,
            dwellMinutes = 30, isActive = true, createdAt = 0
        )
        assertFalse(useCase.isCityMatch("", place))
    }
}
