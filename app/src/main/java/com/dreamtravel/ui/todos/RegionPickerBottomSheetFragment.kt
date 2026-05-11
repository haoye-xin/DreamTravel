package com.dreamtravel.ui.todos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dreamtravel.R
import com.dreamtravel.data.local.RegionDataLoader
import com.dreamtravel.data.model.RegionLevel
import com.dreamtravel.data.model.RegionNode
import com.dreamtravel.data.model.RegionPathMode
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
        const val ARG_PROVINCE_CODE = "province_code"
        const val ARG_CITY_CODE = "city_code"
        const val ARG_DISTRICT_CODE = "district_code"

        fun newInstance(
            provinceCode: String? = null,
            cityCode: String? = null,
            districtCode: String? = null
        ): RegionPickerBottomSheetFragment {
            return RegionPickerBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROVINCE_CODE, provinceCode)
                    putString(ARG_CITY_CODE, cityCode)
                    putString(ARG_DISTRICT_CODE, districtCode)
                }
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

        if (provinceCode != null) {
            val prov = provinces.find { it.code == provinceCode }
            if (prov != null) {
                selectedProvince = prov
                val children = prov.children

                if (children.any { it.level == RegionLevel.CITY }) {
                    pathMode = RegionPathMode.CITY_OPTIONAL_DISTRICT
                    currentList = children
                    currentLevel = RegionLevel.CITY
                    binding.tvPickerTitle.text = getString(R.string.label_select_city)

                    if (cityCode != null) {
                        val second = children.find { it.code == cityCode }
                        if (second != null) {
                            selectedSecondLevel = second
                            if (defaultDistrictCode != null && second.children.isNotEmpty()) {
                                val third = second.children.find { it.code == defaultDistrictCode }
                                if (third != null) {
                                    selectedThirdLevel = third
                                    currentList = second.children
                                    currentLevel = RegionLevel.DISTRICT
                    binding.tvPickerTitle.text = getString(R.string.label_select_district)
                                }
                            }
                        }
                    }
                } else if (children.isNotEmpty()) {
                    pathMode = RegionPathMode.DIRECT_DISTRICT
                    currentList = children
                    currentLevel = RegionLevel.DISTRICT
                    binding.tvPickerTitle.text = getString(R.string.label_select_district)

                    if (cityCode != null) {
                        val second = children.find { it.code == cityCode }
                        if (second != null) {
                            selectedSecondLevel = second
                        }
                    }
                }
            }
        }

        updateBreadcrumb()
        adapter.submitList(currentList) {
            when (currentLevel) {
                RegionLevel.CITY -> adapter.setSelected(selectedSecondLevel?.code)
                RegionLevel.DISTRICT -> {
                    adapter.setSelected(
                        selectedThirdLevel?.code ?: selectedSecondLevel?.code
                    )
                }
                else -> adapter.setSelected(selectedProvince?.code)
            }
        }
        updateConfirmState()
    }

    private fun onRegionSelected(node: RegionNode) {
        when {
            currentLevel == RegionLevel.PROVINCE -> selectProvince(node)
            currentLevel == RegionLevel.CITY -> selectCity(node)
            currentLevel == RegionLevel.DISTRICT && pathMode == RegionPathMode.DIRECT_DISTRICT ->
                selectDirectDistrict(node)
            currentLevel == RegionLevel.DISTRICT -> selectDistrict(node)
        }
    }

    private fun selectProvince(node: RegionNode) {
        selectedProvince = node
        selectedSecondLevel = null
        selectedThirdLevel = null

        val children = node.children
        if (children.isEmpty()) {
            updateBreadcrumb()
            updateConfirmState()
            return
        }

        if (children.any { it.level == RegionLevel.CITY }) {
            pathMode = RegionPathMode.CITY_OPTIONAL_DISTRICT
            currentLevel = RegionLevel.CITY
            currentList = children
                    binding.tvPickerTitle.text = getString(R.string.label_select_city)
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

        val bundle = Bundle().apply {
            putString("provinceCode", prov.code)
            putString("provinceName", prov.name)
            putString("cityCode", second.code)
            putString("cityName", second.name)
            putString("districtCode", third?.code)
            putString("districtName", third?.name)
            putString("formattedAddress", formatted)
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
        val canConfirm = selectedSecondLevel != null
        binding.btnPickerConfirm.isEnabled = canConfirm
    }

    private fun updateBreadcrumb() {
        val breadcrumb = binding.layoutBreadcrumb
        breadcrumb.removeAllViews()

        val provName = selectedProvince?.name ?: getString(R.string.label_province_placeholder)
        addBreadcrumbItem(breadcrumb, provName, isActive = true)

        if (currentLevel == RegionLevel.CITY || currentLevel == RegionLevel.DISTRICT) {
            val secondPlaceholder = if (pathMode == RegionPathMode.DIRECT_DISTRICT) getString(R.string.label_select_district) else getString(R.string.label_select_city)
            val secondName = selectedSecondLevel?.name ?: secondPlaceholder
            addBreadcrumbItem(breadcrumb, secondName, isActive = true)
        }

        if (currentLevel == RegionLevel.DISTRICT && pathMode == RegionPathMode.CITY_OPTIONAL_DISTRICT) {
            val thirdName = selectedThirdLevel?.name ?: getString(R.string.label_select_district)
            addBreadcrumbItem(breadcrumb, thirdName, isActive = selectedThirdLevel != null)
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
