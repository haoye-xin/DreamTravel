package com.dreamtravel.data.local

import android.content.Context
import android.content.res.AssetManager
import com.dreamtravel.data.model.RegionLevel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class RegionDataLoaderTest {

    private val minimalJson = """
        [
            {
                "code": "110000",
                "name": "北京市",
                "level": "PROVINCE",
                "children": [
                    {
                        "code": "110100",
                        "name": "北京市",
                        "level": "CITY",
                        "children": [
                            {"code": "110101", "name": "东城区", "level": "DISTRICT"}
                        ]
                    }
                ]
            },
            {
                "code": "810000",
                "name": "香港特别行政区",
                "level": "PROVINCE",
                "children": [
                    {"code": "810001", "name": "中西区", "level": "DISTRICT"},
                    {"code": "810002", "name": "湾仔区", "level": "DISTRICT"}
                ]
            }
        ]
    """.trimIndent()

    @Test
    fun `loadRegions returns province list`() {
        val loader = createLoader(minimalJson)
        val roots = loader.loadRegions()
        assertEquals(2, roots.size)
        assertEquals("北京市", roots[0].name)
        assertEquals("香港特别行政区", roots[1].name)
    }

    @Test
    fun `province has city children`() {
        val loader = createLoader(minimalJson)
        val roots = loader.loadRegions()
        val beijing = roots.find { it.code == "110000" }!!
        val hasCity = beijing.children.any { it.level == RegionLevel.CITY }
        assertTrue(hasCity)
    }

    @Test
    fun `special region has no city children but has direct districts`() {
        val loader = createLoader(minimalJson)
        val roots = loader.loadRegions()
        val hk = roots.find { it.code == "810000" }!!
        val hasCity = hk.children.any { it.level == RegionLevel.CITY }
        assertFalse(hasCity)
        assertEquals(2, hk.children.size)
        assertEquals("中西区", hk.children[0].name)
        assertEquals(RegionLevel.DISTRICT, hk.children[0].level)
    }

    @Test
    fun `findNodeByCode returns correct node`() {
        val loader = createLoader(minimalJson)
        val node = loader.findNodeByCode("110101")
        assertNotNull(node)
        assertEquals("东城区", node?.name)
    }

    @Test
    fun `findNodeByCode returns null for missing code`() {
        val loader = createLoader(minimalJson)
        val node = loader.findNodeByCode("999999")
        assertNull(node)
    }

    @Test
    fun `loadRegions returns empty list when asset file is missing`() {
        val assets = mockk<AssetManager>(relaxed = true)
        every { assets.open("china_regions.json") } throws java.io.FileNotFoundException("File not found")
        val context = mockk<Context>(relaxed = true)
        every { context.getAssets() } returns assets
        val loader = RegionDataLoader(context)
        val roots = loader.loadRegions()
        assertTrue(roots.isEmpty())
    }

    private fun createLoader(json: String): RegionDataLoader {
        val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        val assets = mockk<AssetManager>(relaxed = true)
        every { assets.open(any()) } returns stream
        val context = mockk<Context>(relaxed = true)
        every { context.getAssets() } returns assets
        return RegionDataLoader(context)
    }
}
