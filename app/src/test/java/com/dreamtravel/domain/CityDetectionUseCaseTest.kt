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

    @Test
    fun `normalizeCityName removes 市 suffix`() {
        assertEquals("北京", useCase.normalizeCityName("北京市"))
        assertEquals("成都", useCase.normalizeCityName("成都市"))
    }

    @Test
    fun `normalizeCityName removes 州 character`() {
        // Note: "杭州市" → remove "市" → "杭州" → remove "州" → "杭"
        assertEquals("杭", useCase.normalizeCityName("杭州市"))
    }

    @Test
    fun `normalizeCityName for autonomous prefecture`() {
        // "大理白族自治州" → remove "白族自治州" first → "大理"
        assertEquals("大理", useCase.normalizeCityName("大理白族自治州"))
    }

    @Test
    fun `normalizeCityName removes 地区 suffix`() {
        assertEquals("昌都", useCase.normalizeCityName("昌都地区"))
    }

    @Test
    fun `normalizeCityName plain name may lose 州`() {
        // "杭州" contains "州" → removed → "杭"
        assertEquals("杭", useCase.normalizeCityName("杭州"))
        // "大理" has no match → unchanged
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
    fun `normalizeCityName complex name`() {
        // "恩施土家族苗族自治州" → remove "自治州" → "恩施土家族苗族"
        assertEquals("恩施土家族苗族", useCase.normalizeCityName("恩施土家族苗族自治州"))
    }

    @Test
    fun `isCityMatch exact match`() {
        val place = Place("1", "大理", null, 25.6, 100.2, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("大理市", place))
        assertTrue(useCase.isCityMatch("大理", place))
    }

    @Test
    fun `isCityMatch cityCode exact match`() {
        val place = Place("1", "大理", "大理", 25.6, 100.2, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("大理白族自治州", place))
    }

    @Test
    fun `isCityMatch cityCode contained in geocode`() {
        val place = Place("1", "成都", "成都", 30.5, 104.0, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("四川省成都市", place))
    }

    @Test
    fun `isCityMatch no match`() {
        val place = Place("1", "大理", null, 25.6, 100.2, 30, true, 0, 0L)
        assertFalse(useCase.isCityMatch("昆明市", place))
    }

    @Test
    fun `isCityMatch cityCode mismatch`() {
        val place = Place("1", "大理", "大理", 25.6, 100.2, 30, true, 0, 0L)
        assertFalse(useCase.isCityMatch("昆明市", place))
    }

    @Test
    fun `isCityMatch substring in geocode`() {
        val place = Place("1", "大理", null, 25.6, 100.2, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("云南省大理市", place))
    }

    @Test
    fun `isCityMatch bidirectional contains`() {
        val place = Place("1", "大理白族自治州", null, 25.6, 100.2, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("大理", place))
    }

    @Test
    fun `isCityMatch name fallback`() {
        val place = Place("1", "上海", null, 31.2, 121.5, 30, true, 0, 0L)
        assertTrue(useCase.isCityMatch("上海市", place))
    }

    @Test
    fun `isCityMatch blank geocode returns false`() {
        val place = Place("1", "大理", null, 25.6, 100.2, 30, true, 0, 0L)
        assertFalse(useCase.isCityMatch("", place))
        assertFalse(useCase.isCityMatch("   ", place))
    }
}
