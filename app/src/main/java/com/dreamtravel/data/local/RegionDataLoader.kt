package com.dreamtravel.data.local

import android.content.Context
import com.dreamtravel.data.model.RegionNode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegionDataLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedRoots: List<RegionNode>? = null
    private var flatMap: Map<String, RegionNode>? = null

    @Synchronized
    fun loadRegions(): List<RegionNode> {
        cachedRoots?.let { return it }

        val roots: List<RegionNode> = try {
            val json = context.assets.open("china_regions.json").use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }
            val type = object : TypeToken<List<RegionNode>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            return emptyList()
        }

        flatMap = buildFlatMap(roots)
        cachedRoots = roots
        return roots
    }

    fun findNodeByCode(code: String): RegionNode? {
        if (flatMap == null) loadRegions()
        return flatMap?.get(code)
    }

    private fun buildFlatMap(nodes: List<RegionNode>): Map<String, RegionNode> {
        val map = mutableMapOf<String, RegionNode>()
        fun collect(node: RegionNode) {
            map[node.code] = node
            node.children?.forEach { collect(it) }
        }
        nodes.forEach { collect(it) }
        return map
    }
}
