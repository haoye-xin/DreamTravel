# China Region Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Taobao-style China administrative region picker to the add/edit todo flow, supporting province/city/optional-district selection with special handling for province-level units without city-level children.

**Architecture:** New `RegionPickerBottomSheetFragment` as a standalone bottom sheet, opened from `AddTodoFragment`. Uses local `assets/china_regions.json` as data source. Result flows back via `FragmentResult`. Region data model classes go into `com.dreamtravel.data.model`. Todo entity/domain gets nullable region fields. DB uses destructive migration so no manual migration class needed.

**Tech Stack:** Kotlin, Room, Hilt, ViewBinding, RecyclerView, coroutines, Material3 BottomSheet, Gson (for JSON parsing — add dependency)

---

### Task 0: Add Gson dependency

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add Gson to dependencies**

In `app/build.gradle.kts`, within the `dependencies` block, add after the existing implementation lines:

```kotlin
    // Gson (JSON parsing for region data)
    implementation("com.google.code.gson:gson:2.10.1")
```

- [ ] **Step 2: Sync and verify**

Run: `.\gradlew :app:dependencies --configuration implementation 2>&1 | Select-String "gson"`
Expected: Shows gson in dependency tree

---

### Task 1: Create region data models

**Files:**
- Create: `app/src/main/java/com/dreamtravel/data/model/Region.kt`

- [ ] **Step 1: Write the Region data model file**

```kotlin
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
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String -Pattern "(BUILD|error)"`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Create china_regions.json asset

**Files:**
- Create: `app/src/main/assets/china_regions.json`

- [ ] **Step 1: Write the region data JSON file**

This is a large file. Write `app/src/main/assets/china_regions.json` with the full China administrative division tree. Structure:

```json
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
          { "code": "110101", "name": "东城区", "level": "DISTRICT" },
          { "code": "110102", "name": "西城区", "level": "DISTRICT" }
        ]
      }
    ]
  }
]
```

Include all 34 province-level units with full city/district children. Key special cases:
- 北京市(110000) → 北京市(110100) as CITY → districts
- 上海市(310000) → 上海市(310100) as CITY → districts
- 天津市(120000) → 天津市(120100) as CITY → districts
- 重庆市(500000) → 重庆市(500100) as CITY → districts
- 香港特别行政区(810000) → direct district children (no CITY level)
- 澳门特别行政区(820000) → direct district children (no CITY level)
- 台湾省(710000) → cities → districts

- [ ] **Step 2: Verify asset exists and is valid JSON**

Run: `Get-Content -Path "app\src\main\assets\china_regions.json" -First 3`
Expected: Shows opening bracket and first province entry

---

### Task 3: Create region data loader

**Files:**
- Create: `app/src/main/java/com/dreamtravel/data/local/RegionDataLoader.kt`

- [ ] **Step 1: Write the RegionDataLoader**

```kotlin
package com.dreamtravel.data.local

import android.content.Context
import com.dreamtravel.data.model.RegionLevel
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

    fun loadRegions(): List<RegionNode> {
        cachedRoots?.let { return it }

        val json = context.assets.open("china_regions.json").use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
        }

        val type = object : TypeToken<List<RegionNode>>() {}.type
        val roots: List<RegionNode> = Gson().fromJson(json, type)

        cachedRoots = roots
        flatMap = buildFlatMap(roots)
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
            node.children.forEach { collect(it) }
        }
        nodes.forEach { collect(it) }
        return map
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String -Pattern "(BUILD|error)"`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Extend TodoEntity with region fields

**Files:**
- Modify: `app/src/main/java/com/dreamtravel/data/local/entity/Entity.kt`

- [ ] **Step 1: Add region fields to TodoEntity**

Replace the existing `TodoEntity` class in `Entity.kt` (lines 21-34) with:

```kotlin
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val title: String,
    val notes: String = "",
    val status: String = "PENDING",
    val remindIntervalMinutes: Int = 1440,
    val remindCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val provinceCode: String? = null,
    val provinceName: String? = null,
    val cityCode: String? = null,
    val cityName: String? = null,
    val districtCode: String? = null,
    val districtName: String? = null,
    val formattedAddress: String? = null
)
```

- [ ] **Step 2: Bump database version**

In `app/src/main/java/com/dreamtravel/data/local/AppDatabase.kt`, change `version = 2` to `version = 3`.

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String -Pattern "(BUILD|error)"`
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Extend Todo domain model with region fields

**Files:**
- Modify: `app/src/main/java/com/dreamtravel/data/model/Todo.kt`

- [ ] **Step 1: Add region fields to Todo data class**

Replace the existing `Todo` data class (lines 16-27) with:

```kotlin
data class Todo(
    val id: String,
    val placeId: String,
    val title: String,
    val notes: String,
    val status: TodoStatus,
    val remindIntervalMinutes: Int,
    val remindCount: Int,
    val createdAt: Long,
    val completedAt: Long?,
    val updatedAt: Long,
    val provinceCode: String? = null,
    val provinceName: String? = null,
    val cityCode: String? = null,
    val cityName: String? = null,
    val districtCode: String? = null,
    val districtName: String? = null,
    val formattedAddress: String? = null
)
```

- [ ] **Step 2: Update toDomain() extension function**

Replace the existing `TodoEntity.toDomain()` (lines 29-40) with:

```kotlin
fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    placeId = placeId,
    title = title,
    notes = notes,
    status = TodoStatus.from(status),
    remindIntervalMinutes = remindIntervalMinutes,
    remindCount = remindCount,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    provinceCode = provinceCode,
    provinceName = provinceName,
    cityCode = cityCode,
    cityName = cityName,
    districtCode = districtCode,
    districtName = districtName,
    formattedAddress = formattedAddress
)
```

- [ ] **Step 3: Update toEntity() extension function**

Replace the existing `Todo.toEntity()` (lines 42-53) with:

```kotlin
fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    placeId = placeId,
    title = title,
    notes = notes,
    status = status.value,
    remindIntervalMinutes = remindIntervalMinutes,
    remindCount = remindCount,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    provinceCode = provinceCode,
    provinceName = provinceName,
    cityCode = cityCode,
    cityName = cityName,
    districtCode = districtCode,
    districtName = districtName,
    formattedAddress = formattedAddress
)
```

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String -Pattern "(BUILD|error)"`
Expected: `BUILD SUCCESSFUL`

---

### Task 6: Create RegionPicker layout XML

**Files:**
- Create: `app/src/main/res/layout/fragment_region_picker.xml`

- [ ] **Step 1: Write the layout file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingTop="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp"
        android:paddingBottom="8dp"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/tvPickerTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/label_select_region"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/btnPickerConfirm"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/btn_confirm"
            android:enabled="false" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/layoutBreadcrumb"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp"
        android:paddingBottom="4dp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvRegionList"
        android:layout_width="match_parent"
        android:layout_height="400dp"
        android:clipToPadding="false" />
</LinearLayout>
```

---

### Task 7: Create region picker item layout

**Files:**
- Create: `app/src/main/res/layout/item_region.xml`

- [ ] **Step 1: Write the item layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="14dp"
    android:gravity="center_vertical"
    android:background="?attr/selectableItemBackground">

    <TextView
        android:id="@+id/tvRegionName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="16sp" />

    <ImageView
        android:id="@+id/ivSelected"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_check"
        android:visibility="gone"
        android:contentDescription="@null" />
</LinearLayout>
```

- [ ] **Step 2: Create check drawable**

Create `app/src/main/res/drawable/ic_check.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorPrimary">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z" />
</vector>
```

---

### Task 8: Create RegionAdapter

**Files:**
- Create: `app/src/main/java/com/dreamtravel/ui/todos/RegionAdapter.kt`

- [ ] **Step 1: Write the adapter**

```kotlin
package com.dreamtravel.ui.todos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dreamtravel.data.model.RegionNode
import com.dreamtravel.databinding.ItemRegionBinding

class RegionAdapter(
    private val onItemClick: (RegionNode) -> Unit
) : ListAdapter<RegionNode, RegionAdapter.ViewHolder>(DiffCallback) {

    private var selectedCode: String? = null

    fun setSelected(code: String?) {
        val old = selectedCode
        selectedCode = code
        old?.let { notifyItemChangedByCode(it) }
        code?.let { notifyItemChangedByCode(it) }
    }

    private fun notifyItemChangedByCode(code: String) {
        val index = currentList.indexOfFirst { it.code == code }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).code == selectedCode)
    }

    class ViewHolder(
        private val binding: ItemRegionBinding,
        private val onItemClick: (RegionNode) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: RegionNode? = null

        init {
            binding.root.setOnClickListener {
                currentItem?.let { onItemClick(it) }
            }
        }

        fun bind(item: RegionNode, isSelected: Boolean) {
            currentItem = item
            binding.tvRegionName.text = item.name
            binding.ivSelected.visibility = if (isSelected) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RegionNode>() {
        override fun areItemsTheSame(oldItem: RegionNode, newItem: RegionNode): Boolean =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: RegionNode, newItem: RegionNode): Boolean =
            oldItem == newItem
    }
}
```

---

### Task 9: Create RegionPickerBottomSheetFragment

**Files:**
- Create: `app/src/main/java/com/dreamtravel/ui/todos/RegionPickerBottomSheetFragment.kt`

- [ ] **Step 1: Write the picker fragment**

```kotlin
package com.dreamtravel.ui.todos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.dreamtravel.data.local.RegionDataLoader
import com.dreamtravel.data.model.RegionLevel
import com.dreamtravel.data.model.RegionNode
import com.dreamtravel.data.model.RegionPathMode
import com.dreamtravel.data.model.SelectedRegion
import com.dreamtravel.databinding.FragmentRegionPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegionPickerBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject lateinit var regionDataLoader: RegionDataLoader

    private var _binding: FragmentRegionPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RegionAdapter

    private var provinces: List<RegionNode> = emptyList()
    private var selectedProvince: RegionNode? = null
    private var selectedSecondLevel: RegionNode? = null
    private var selectedThirdLevel: RegionNode? = null
    private var pathMode: RegionPathMode? = null
    private var currentList: List<RegionNode> = emptyList()
    private var currentLevel: RegionLevel = RegionLevel.PROVINCE

    private var defaultProvinceCode: String? = null
    private var defaultCityCode: String? = null
    private var defaultDistrictCode: String? = null

    companion object {
        const val REQUEST_KEY = "region_picker"
        const val RESULT_KEY = "selected_region"
        const val ARG_PROVINCE_CODE = "province_code"
        const val ARG_CITY_CODE = "city_code"
        const val ARG_DISTRICT_CODE = "district_code"

        fun newInstance(
            provinceCode: String? = null,
            cityCode: String? = null,
            districtCode: String? = null
        ): RegionPickerBottomSheetFragment {
            return RegionPickerBottomSheetFragment().apply {
                defaultProvinceCode = provinceCode
                defaultCityCode = cityCode
                defaultDistrictCode = districtCode
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            defaultProvinceCode = it.getString(ARG_PROVINCE_CODE)
            defaultCityCode = it.getString(ARG_CITY_CODE)
            defaultDistrictCode = it.getString(ARG_DISTRICT_CODE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegionPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RegionAdapter { node -> onRegionSelected(node) }
        binding.rvRegionList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegionList.adapter = adapter

        binding.btnPickerConfirm.setOnClickListener { confirmSelection() }

        provinces = regionDataLoader.loadRegions()
        restoreDefaultPath()
    }

    private fun restoreDefaultPath() {
        val provinceCode = defaultProvinceCode
        val cityCode = defaultCityCode
        val districtCode = defaultDistrictCode

        if (provinceCode != null) {
            val prov = provinces.find { it.code == provinceCode }
            if (prov != null) {
                selectedProvince = prov
                val children = prov.children

                if (children.any { it.level == RegionLevel.CITY }) {
                    pathMode = RegionPathMode.CITY_OPTIONAL_DISTRICT
                    currentList = children
                    currentLevel = RegionLevel.CITY
                    binding.tvPickerTitle.text = "选择城市"

                    if (cityCode != null) {
                        val city = children.find { it.code == cityCode }
                        if (city != null) {
                            selectedSecondLevel = city
                            if (districtCode != null && city.children.isNotEmpty()) {
                                val dist = city.children.find { it.code == districtCode }
                                if (dist != null) {
                                    selectedThirdLevel = dist
                                    currentList = city.children
                                    currentLevel = RegionLevel.DISTRICT
                                    binding.tvPickerTitle.text = "选择区/县"
                                }
                            }
                        }
                    }
                } else if (children.isNotEmpty()) {
                    pathMode = RegionPathMode.DIRECT_DISTRICT
                    currentList = children
                    currentLevel = RegionLevel.DISTRICT
                    binding.tvPickerTitle.text = "选择区/县"

                    if (cityCode != null) {
                        val dist = children.find { it.code == cityCode }
                        if (dist != null) {
                            selectedSecondLevel = dist
                        }
                    }
                }
            }
        }

        updateBreadcrumb()
        adapter.submitList(currentList) {
            when (currentLevel) {
                RegionLevel.CITY -> adapter.setSelected(selectedSecondLevel?.code)
                RegionLevel.DISTRICT -> adapter.setSelected(selectedThirdLevel?.code ?: selectedSecondLevel?.code)
                else -> adapter.setSelected(selectedProvince?.code)
            }
        }
        updateConfirmState()
    }

    private fun onRegionSelected(node: RegionNode) {
        when {
            currentLevel == RegionLevel.PROVINCE -> selectProvince(node)
            currentLevel == RegionLevel.CITY -> selectCity(node)
            currentLevel == RegionLevel.DISTRICT -> {
                if (pathMode == RegionPathMode.DIRECT_DISTRICT) {
                    selectDirectDistrict(node)
                } else {
                    selectDistrict(node)
                }
            }
        }
    }

    private fun selectProvince(node: RegionNode) {
        selectedProvince = node
        selectedSecondLevel = null
        selectedThirdLevel = null

        val children = node.children
        if (children.isEmpty()) {
            // No children — cannot proceed
            return
        }

        if (children.any { it.level == RegionLevel.CITY }) {
            pathMode = RegionPathMode.CITY_OPTIONAL_DISTRICT
            currentLevel = RegionLevel.CITY
            currentList = children
            binding.tvPickerTitle.text = "选择城市"
        } else {
            pathMode = RegionPathMode.DIRECT_DISTRICT
            currentLevel = RegionLevel.DISTRICT
            currentList = children
            binding.tvPickerTitle.text = "选择区/县"
        }

        updateBreadcrumb()
        adapter.submitList(currentList)
        adapter.setSelected(null)
        updateConfirmState()
    }

    private fun selectCity(node: RegionNode) {
        selectedSecondLevel = node
        selectedThirdLevel = null
        adapter.setSelected(node.code)
        updateConfirmState()

        if (node.children.isNotEmpty()) {
            // Has districts — keep current list for now, user needs to tap to continue
            // Actually, for better UX we auto-navigate to districts on tap
            // But the design says: user can confirm at city level OR continue to district
            // So on tap, we show districts immediately:
            currentLevel = RegionLevel.DISTRICT
            currentList = node.children
            binding.tvPickerTitle.text = "选择区/县"
            updateBreadcrumb()
            adapter.submitList(currentList)
            adapter.setSelected(null)
        }
    }

    private fun selectDistrict(node: RegionNode) {
        selectedThirdLevel = node
        adapter.setSelected(node.code)
        updateConfirmState()
    }

    private fun selectDirectDistrict(node: RegionNode) {
        selectedSecondLevel = node
        selectedThirdLevel = null
        adapter.setSelected(node.code)
        updateConfirmState()
    }

    private fun confirmSelection() {
        val prov = selectedProvince ?: return
        val second = selectedSecondLevel ?: return

        val third = selectedThirdLevel

        val formatted = buildFormattedAddress(prov.name, second.name, third?.name)

        val result = SelectedRegion(
            provinceCode = prov.code,
            provinceName = prov.name,
            cityCode = second.code,
            cityName = second.name,
            districtCode = third?.code,
            districtName = third?.name,
            formattedAddress = formatted
        )

        val bundle = Bundle().apply {
            putString("provinceCode", result.provinceCode)
            putString("provinceName", result.provinceName)
            putString("cityCode", result.cityCode)
            putString("cityName", result.cityName)
            putString("districtCode", result.districtCode)
            putString("districtName", result.districtName)
            putString("formattedAddress", result.formattedAddress)
        }

        parentFragmentManager.setFragmentResult(REQUEST_KEY, bundle)
        dismiss()
    }

    private fun buildFormattedAddress(
        province: String,
        second: String,
        third: String?
    ): String {
        val parts = mutableListOf<String>()
        if (province == second) {
            parts.add(province)
        } else {
            parts.add(province)
            parts.add(second)
        }
        third?.let { if (it != second) parts.add(it) }
        return parts.joinToString("")
    }

    private fun updateConfirmState() {
        val canConfirm = when (pathMode) {
            RegionPathMode.CITY_OPTIONAL_DISTRICT -> selectedSecondLevel != null
            RegionPathMode.DIRECT_DISTRICT -> selectedSecondLevel != null
            null -> false
        }
        binding.btnPickerConfirm.isEnabled = canConfirm
    }

    private fun updateBreadcrumb() {
        val breadcrumb = binding.layoutBreadcrumb
        breadcrumb.removeAllViews()

        val provName = selectedProvince?.name ?: "省/自治区/直辖市"
        addBreadcrumbItem(breadcrumb, provName, isActive = true)

        if (currentLevel == RegionLevel.CITY || currentLevel == RegionLevel.DISTRICT) {
            val cityName = selectedSecondLevel?.name ?: "选择城市"
            addBreadcrumbItem(breadcrumb, cityName, isActive = true)
        }

        if (currentLevel == RegionLevel.DISTRICT && pathMode == RegionPathMode.CITY_OPTIONAL_DISTRICT) {
            val distName = selectedThirdLevel?.name ?: "选择区/县"
            addBreadcrumbItem(breadcrumb, distName, isActive = selectedThirdLevel != null)
        }
    }

    private fun addBreadcrumbItem(parent: ViewGroup, text: String, isActive: Boolean) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setPadding(4, 4, 4, 4)
        }
        if (parent.childCount > 0) {
            val arrow = TextView(requireContext()).apply {
                this.text = " > "
                textSize = 13f
                setPadding(4, 4, 4, 4)
            }
            parent.addView(arrow)
        }
        parent.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

### Task 10: Update fragment_add_todo.xml with region row

**Files:**
- Modify: `app/src/main/res/layout/fragment_add_todo.xml`

- [ ] **Step 1: Add region selection row to todo form**

Replace the entire content of `fragment_add_todo.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/editTodoTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/label_todo_title"
        android:inputType="text" />

    <EditText
        android:id="@+id/editTodoNotes"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:hint="@string/label_todo_notes"
        android:inputType="textMultiLine"
        android:minLines="2" />

    <LinearLayout
        android:id="@+id/layoutRegionSelector"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingVertical="12dp"
        android:paddingHorizontal="4dp"
        android:background="?attr/selectableItemBackground"
        android:clickable="true"
        android:focusable="true">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/label_city"
            android:textSize="16sp"
            android:textColor="?android:attr/textColorPrimary" />

        <TextView
            android:id="@+id/tvSelectedRegion"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/hint_select_city"
            android:textSize="14sp"
            android:textColor="?android:attr/textColorSecondary" />

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:layout_marginStart="8dp"
            android:src="@drawable/ic_chevron_right"
            android:contentDescription="@null" />
    </LinearLayout>

    <Spinner
        android:id="@+id/spinnerRemindInterval"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:prompt="@string/label_remind_interval" />

    <Button
        android:id="@+id/btnSaveTodo"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/btn_save" />

</LinearLayout>
```

- [ ] **Step 2: Create chevron drawable**

Create `app/src/main/res/drawable/ic_chevron_right.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?android:attr/textColorTertiary">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z" />
</vector>
```

---

### Task 11: Update AddTodoViewModel with region state

**Files:**
- Modify: `app/src/main/java/com/dreamtravel/ui/todos/AddTodoViewModel.kt`

- [ ] **Step 1: Add region state and update save methods**

Replace the entire `AddTodoViewModel.kt` file (107 lines) with:

```kotlin
package com.dreamtravel.ui.todos

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.model.Todo
import com.dreamtravel.data.model.TodoStatus
import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTodoViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager
) : ViewModel() {

    val placeId: String = savedStateHandle["placeId"] ?: ""
    val todoId: String = savedStateHandle["todoId"] ?: ""

    val isEditing: Boolean get() = todoId.isNotBlank()

    private val _existingTodo = MutableStateFlow<Todo?>(null)
    val existingTodo: StateFlow<Todo?> = _existingTodo.asStateFlow()

    private val _defaultRemindInterval = MutableStateFlow(defaultRemindInterval())
    val defaultRemindInterval: StateFlow<Int> = _defaultRemindInterval.asStateFlow()

    private val _selectedRegion = MutableStateFlow<SavedRegion?>(null)
    val selectedRegion: StateFlow<SavedRegion?> = _selectedRegion.asStateFlow()

    init {
        if (isEditing) {
            loadTodo()
        }
    }

    private fun loadTodo() {
        viewModelScope.launch {
            val todo = repository.getTodoById(todoId)
            _existingTodo.value = todo
            todo?.let { t ->
                if (t.provinceCode != null && t.formattedAddress != null) {
                    _selectedRegion.value = SavedRegion(
                        provinceCode = t.provinceCode,
                        provinceName = t.provinceName,
                        cityCode = t.cityCode,
                        cityName = t.cityName,
                        districtCode = t.districtCode,
                        districtName = t.districtName,
                        formattedAddress = t.formattedAddress
                    )
                }
            }
        }
    }

    fun onRegionSelected(
        provinceCode: String?,
        provinceName: String?,
        cityCode: String?,
        cityName: String?,
        districtCode: String?,
        districtName: String?,
        formattedAddress: String?
    ) {
        if (provinceCode == null || formattedAddress == null) return
        _selectedRegion.value = SavedRegion(
            provinceCode = provinceCode,
            provinceName = provinceName,
            cityCode = cityCode,
            cityName = cityName,
            districtCode = districtCode,
            districtName = districtName,
            formattedAddress = formattedAddress
        )
    }

    suspend fun addTodoSuspend(title: String, notes: String, remindIntervalMinutes: Int) {
        saveDefaultRemindInterval(remindIntervalMinutes)
        val region = _selectedRegion.value
        repository.addTodo(
            Todo(
                id = UUID.randomUUID().toString(),
                placeId = placeId,
                title = title,
                notes = notes,
                status = TodoStatus.PENDING,
                remindIntervalMinutes = remindIntervalMinutes,
                remindCount = 0,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                updatedAt = System.currentTimeMillis(),
                provinceCode = region?.provinceCode,
                provinceName = region?.provinceName,
                cityCode = region?.cityCode,
                cityName = region?.cityName,
                districtCode = region?.districtCode,
                districtName = region?.districtName,
                formattedAddress = region?.formattedAddress
            )
        )
        analytics.logEvent(AnalyticsEvent.TODO_ADDED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
    }

    suspend fun updateTodoSuspend(title: String, notes: String, remindIntervalMinutes: Int) {
        saveDefaultRemindInterval(remindIntervalMinutes)
        val existing = _existingTodo.value ?: return
        val region = _selectedRegion.value
        repository.updateTodo(
            existing.copy(
                title = title,
                notes = notes,
                remindIntervalMinutes = remindIntervalMinutes,
                updatedAt = System.currentTimeMillis(),
                provinceCode = region?.provinceCode ?: existing.provinceCode,
                provinceName = region?.provinceName ?: existing.provinceName,
                cityCode = region?.cityCode ?: existing.cityCode,
                cityName = region?.cityName ?: existing.cityName,
                districtCode = region?.districtCode ?: existing.districtCode,
                districtName = region?.districtName ?: existing.districtName,
                formattedAddress = region?.formattedAddress ?: existing.formattedAddress
            )
        )
        analytics.logEvent(AnalyticsEvent.TODO_EDITED, mapOf(AnalyticsEvent.Param.PLACE_ID to placeId))
    }

    private fun defaultRemindInterval(): Int {
        return prefs().getInt(PREF_KEY_REMIND_INTERVAL, Constants.DEFAULT_REMIND_INTERVAL_MINUTES)
    }

    private fun saveDefaultRemindInterval(minutes: Int) {
        prefs().edit().putInt(PREF_KEY_REMIND_INTERVAL, minutes).apply()
    }

    private fun prefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "dream_travel_prefs"
        private const val PREF_KEY_REMIND_INTERVAL = "default_remind_interval_minutes"
    }
}

data class SavedRegion(
    val provinceCode: String?,
    val provinceName: String?,
    val cityCode: String?,
    val cityName: String?,
    val districtCode: String?,
    val districtName: String?,
    val formattedAddress: String?
)
```

---

### Task 12: Update AddTodoFragment with region picker integration

**Files:**
- Modify: `app/src/main/java/com/dreamtravel/ui/todos/AddTodoFragment.kt`

- [ ] **Step 1: Rewrite AddTodoFragment with region picker support**

Replace the entire `AddTodoFragment.kt` file (159 lines) with:

```kotlin
package com.dreamtravel.ui.todos

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentAddTodoBinding
import com.dreamtravel.util.PermissionUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

data class RemindIntervalOption(val label: String, val minutes: Int)

@AndroidEntryPoint
class AddTodoFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTodoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTodoViewModel by viewModels()
    private var notificationRequested = false

    private val remindOptions = listOf(
        RemindIntervalOption("30分钟", 30),
        RemindIntervalOption("1小时", 60),
        RemindIntervalOption("6小时", 360),
        RemindIntervalOption("24小时", 1440),
        RemindIntervalOption("48小时", 2880)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRemindIntervalSpinner()
        observeEditMode()
        observeRegion()

        binding.layoutRegionSelector.setOnClickListener {
            openRegionPicker()
        }

        binding.btnSaveTodo.setOnClickListener {
            val title = binding.editTodoTitle.text.toString().trim()
            if (title.isBlank()) {
                binding.editTodoTitle.error = "请输入待办事项"
                return@setOnClickListener
            }

            val notes = binding.editTodoNotes.text.toString().trim()
            val selectedOption = remindOptions[binding.spinnerRemindInterval.selectedItemPosition]
            val remindIntervalMinutes = selectedOption.minutes

            requestNotificationIfNeeded()

            viewLifecycleOwner.lifecycleScope.launch {
                if (viewModel.isEditing) {
                    viewModel.updateTodoSuspend(title, notes, remindIntervalMinutes)
                } else {
                    viewModel.addTodoSuspend(title, notes, remindIntervalMinutes)
                }
                dismiss()
            }
        }

        parentFragmentManager.setFragmentResultListener(
            RegionPickerBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            viewModel.onRegionSelected(
                provinceCode = bundle.getString("provinceCode"),
                provinceName = bundle.getString("provinceName"),
                cityCode = bundle.getString("cityCode"),
                cityName = bundle.getString("cityName"),
                districtCode = bundle.getString("districtCode"),
                districtName = bundle.getString("districtName"),
                formattedAddress = bundle.getString("formattedAddress")
            )
        }
    }

    private fun openRegionPicker() {
        val currentRegion = viewModel.selectedRegion.value
        val picker = RegionPickerBottomSheetFragment.newInstance(
            provinceCode = currentRegion?.provinceCode,
            cityCode = currentRegion?.cityCode,
            districtCode = currentRegion?.districtCode
        )
        picker.show(parentFragmentManager, "region_picker")
    }

    private fun observeRegion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedRegion.collect { region ->
                    if (region?.formattedAddress != null) {
                        binding.tvSelectedRegion.text = region.formattedAddress
                        binding.tvSelectedRegion.setTextColor(
                            requireContext().getColor(android.R.color.black)
                        )
                    } else {
                        binding.tvSelectedRegion.text = getString(R.string.hint_select_city)
                        binding.tvSelectedRegion.setTextColor(
                            requireContext().getColor(android.R.color.darker_gray)
                        )
                    }
                }
            }
        }
    }

    private fun setupRemindIntervalSpinner() {
        val labels = remindOptions.map { it.label }.toTypedArray()
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.text = labels[position]
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.text = labels[position]
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRemindInterval.adapter = adapter
    }

    private fun observeEditMode() {
        if (!viewModel.isEditing) {
            binding.btnSaveTodo.text = getString(R.string.btn_save)
            selectDefaultRemindInterval()
            return
        }

        binding.btnSaveTodo.text = getString(R.string.btn_update)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.existingTodo.collect { todo ->
                    todo?.let {
                        binding.editTodoTitle.setText(it.title)
                        if (it.notes.isNotBlank()) {
                            binding.editTodoNotes.setText(it.notes)
                        }
                        val minutes = it.remindIntervalMinutes
                        val index = remindOptions.indexOfFirst { opt -> opt.minutes == minutes }
                        if (index >= 0) {
                            binding.spinnerRemindInterval.setSelection(index)
                        }
                    }
                }
            }
        }
    }

    private fun selectDefaultRemindInterval() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.defaultRemindInterval.collect { minutes ->
                    val index = remindOptions.indexOfFirst { opt -> opt.minutes == minutes }
                    if (index >= 0) {
                        binding.spinnerRemindInterval.setSelection(index)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (PermissionUtils.hasNotificationPermission(requireContext())) return
        if (notificationRequested) return

        val perms = PermissionUtils.getNotificationPermission()
        if (perms.isNotEmpty()) {
            notificationRequested = true
            requestPermissions(perms, 200)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

### Task 13: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add new string resources**

Add the following strings to `strings.xml` before the closing `</resources>` tag:

```xml
    <string name="label_city">城市</string>
    <string name="hint_select_city">请选择城市</string>
    <string name="label_select_region">选择地区</string>
    <string name="btn_confirm">确认</string>
```

---

### Task 14: Write unit tests for RegionDataLoader

**Files:**
- Create: `app/src/test/java/com/dreamtravel/data/local/RegionDataLoaderTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package com.dreamtravel.data.local

import android.content.Context
import android.content.res.AssetManager
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
        val hasCity = beijing.children.any { it.level == com.dreamtravel.data.model.RegionLevel.CITY }
        assertTrue(hasCity)
    }

    @Test
    fun `special region has no city children`() {
        val loader = createLoader(minimalJson)
        val roots = loader.loadRegions()
        val hk = roots.find { it.code == "810000" }!!
        val hasCity = hk.children.any { it.level == com.dreamtravel.data.model.RegionLevel.CITY }
        assertFalse(hasCity)
        assertEquals("中西区", hk.children[0].name)
        assertEquals(com.dreamtravel.data.model.RegionLevel.DISTRICT, hk.children[0].level)
    }

    @Test
    fun `findNodeByCode returns correct node`() {
        val loader = createLoader(minimalJson)
        val node = loader.findNodeByCode("110101")
        assertNotNull(node)
        assertEquals("东城区", node?.name)
    }

    @Test
    fun `findNodeByCode returns null for missing`() {
        val loader = createLoader(minimalJson)
        val node = loader.findNodeByCode("999999")
        assertNull(node)
    }

    private fun createLoader(json: String): RegionDataLoader {
        val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        val assets = mockk<AssetManager> {
            every { open("china_regions.json") } returns stream
        }
        val context = mockk<Context> {
            every { assets } returns this@mockk.assets
        }
        return RegionDataLoader(context)
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dreamtravel.data.local.RegionDataLoaderTest"`
Expected: All 5 tests pass

---

### Task 15: Run existing tests to verify no regressions

**Files:**
- None (verification only)

- [ ] **Step 1: Run all unit tests**

Run: `.\gradlew :app:testDebugUnitTest`
Expected: All tests pass (including existing DreamRepositoryImplTest which creates Todo objects)

---

### Task 16: Full build verification

**Files:**
- None (verification only)

- [ ] **Step 1: Run full debug build**

Run: `.\gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

---

## Self-Review

**Spec coverage check:**
- Province -> city confirm works: Task 9 (`selectCity` enables confirm)
- Province -> city -> district confirm works: Task 9 (`selectDistrict` + `confirmSelection`)
- Changing province resets lower levels: Task 9 (`selectProvince` clears second+third)
- Changing city resets district: Task 9 (`selectCity` clears thirdLevel)
- Direct province -> district path: Task 9 (`selectProvince` detects DIRECT_DISTRICT mode)
- Direct path persists into cityCode/cityName: Task 9 (`confirmSelection` maps second.code → cityCode)
- Edit mode restores: Task 9 (`restoreDefaultPath`), Task 11 (`loadTodo`)
- Formatted address de-duplication: Task 9 (`buildFormattedAddress`)
- Dataset gaps don't crash: Task 9 (empty children check in `selectProvince`)
- Cancel resets: Readme spec says picker state discarded on dismiss; BottomSheetDialogFragment by default doesn't persist state without confirm — this is inherent behavior

**Placeholder scan:** No TODOs, no TBDs, no placeholders.

**Type consistency:** `SavedRegion` defined in Task 11, used in Task 12. `RegionNode`, `SelectedRegion`, `RegionPathMode` defined in Task 1, used in Tasks 8, 9, 11.
