# China Region Picker

## Overview

This document describes the design for a China administrative region picker used in the add/edit todo flow.

The current todo form lives in `AddTodoFragment` and is implemented as a `BottomSheetDialogFragment`. The app does not currently have an address selector.

The goal is to add a Taobao-style administrative region selection flow without detailed street address input. Users should select a province-level administrative unit first, then continue to the next valid level. In normal cases that means city-level. For special province-level units that do not have city-level children, the flow should skip city and go directly to district/county-level selection.

## Goals

- add a region entry point to the add/edit todo sheet
- open a dedicated region picker bottom sheet
- support save at city level or district level where applicable
- support direct province -> district selection when there is no city layer
- support edit-mode default restoration
- reset lower-level selection when parent selection changes
- persist the result in the required field shape

## Out of Scope

- street address
- house number
- free-form detailed address input
- online region data syncing
- picker search

## Required Result Shape

The saved result must contain:

```text
provinceCode
provinceName
cityCode
cityName
districtCode
districtName
formattedAddress
```

Rules:

- `districtCode` can be empty
- `districtName` can be empty
- users are not allowed to save only the province-level selection

## Interaction Design

### Todo Form Entry

Add a single row to the existing add/edit todo form.

- label: `城市`
- empty state: `请选择城市`
- selected state: display `formattedAddress`
- interaction: tap to open the region picker bottom sheet

This row behaves like a picker entry, not a text input.

### Picker Container

Use a dedicated `RegionPickerBottomSheetFragment`.

Top area:

- title: `选择地区`
- close action
- confirm action

Body area:

- dynamic level indicator or breadcrumb
- one list for the current level

The picker should not hard-code the UI as always `省 / 市 / 区县`. It needs to adapt to two possible paths.

### Path A: Province Has City-Level Children

Flow:

1. user selects province
2. picker opens city list
3. user selects city
4. user can confirm immediately and save at city level
5. if the city has district children, user can continue selecting district and then confirm

Confirm behavior:

- province only: cannot confirm
- province + city: can confirm
- province + city + district: can confirm

### Path B: Province Has No City-Level Children

Flow:

1. user selects province
2. picker detects there is no city layer
3. picker skips city step and opens district/county list directly
4. user must choose one second-level item before confirm is enabled

Confirm behavior:

- province only: cannot confirm
- province + district: can confirm

This rule applies to special province-level units only when their dataset really has no city-level children. If city-level children exist, the flow remains unchanged.

### Cancel Behavior

If the picker is dismissed without tapping confirm:

- temporary picker state is discarded
- todo form remains unchanged

Only confirmed selection is written back.

### Edit Restoration

When editing an existing todo with saved region data:

- the todo form displays `formattedAddress`
- opening the picker restores the existing path
- if a district exists, restore the district selection
- if only city-level data exists, restore the city selection
- if the path was direct province -> district, restore that second-level district selection

## Data Model

### Runtime Region Tree

```kotlin
enum class RegionLevel {
    PROVINCE,
    CITY,
    DISTRICT
}

data class RegionNode(
    val code: String,
    val name: String,
    val level: RegionLevel,
    val parentCode: String?,
    val children: List<RegionNode> = emptyList()
)
```

This model reflects the real hierarchy and avoids inventing fake city-level nodes.

### Confirmed Selection Result

```kotlin
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

### Path Mode

The picker should track which structural path is active.

```kotlin
enum class RegionPathMode {
    CITY_OPTIONAL_DISTRICT,
    DIRECT_DISTRICT
}
```

This is runtime state and does not need to be persisted if it can be reconstructed from saved codes and region data.

## Persistence Mapping

### Normal Path

For `province -> city -> optional district`:

- `provinceCode` = province code
- `provinceName` = province name
- `cityCode` = city code
- `cityName` = city name
- `districtCode` = district code or `null`
- `districtName` = district name or `null`

### Direct Province -> District Path

For `province -> district` where there is no city layer:

- `provinceCode` = province code
- `provinceName` = province name
- `cityCode` = selected second-level district code
- `cityName` = selected second-level district name
- `districtCode` = `null`
- `districtName` = `null`

This is the key compatibility rule. The second valid level under the province is persisted into `cityCode` and `cityName` when there is no real city layer.

This keeps the required output contract unchanged and avoids fabricating a fake city node.

## Formatted Address Rules

Suggested formatting:

- city-level save: `provinceName + cityName`
- district-level save on normal path: `provinceName + cityName + districtName`
- direct province -> district save: `provinceName + cityName`

De-duplicate repeated municipality names:

- `北京市 + 北京市` -> `北京市`
- `北京市 + 北京市 + 朝阳区` -> `北京市朝阳区`

Examples:

- `广东省深圳市`
- `广东省深圳市南山区`
- `香港特别行政区中西区`

## Region Data Source

Use a bundled local dataset such as `assets/china_regions.json`.

Reasons:

- no network dependency
- stable foundational data
- simpler picker behavior

Recommended JSON shape:

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
          {
            "code": "110101",
            "name": "东城区",
            "level": "DISTRICT"
          }
        ]
      }
    ]
  }
]
```

The picker can load this data into memory and reuse it across sessions.

## State Management

### Add/Edit Todo State

`AddTodoViewModel` should own the final confirmed region state.

Suggested addition:

```kotlin
private val _selectedRegion = MutableStateFlow<SelectedRegion?>(null)
val selectedRegion: StateFlow<SelectedRegion?> = _selectedRegion
```

Responsibilities:

- expose the currently confirmed region for UI display
- restore saved region when editing
- provide region fields when saving or updating the todo

### Picker Temporary State

The picker manages temporary selection internally so cancel does not mutate the form.

Suggested UI state:

```kotlin
data class RegionPickerUiState(
    val provinces: List<RegionNode> = emptyList(),
    val secondLevelOptions: List<RegionNode> = emptyList(),
    val thirdLevelOptions: List<RegionNode> = emptyList(),
    val selectedProvince: RegionNode? = null,
    val selectedSecondLevel: RegionNode? = null,
    val selectedThirdLevel: RegionNode? = null,
    val pathMode: RegionPathMode? = null
)
```

### State Transition Rules

Initial load:

1. load province list
2. if edit defaults exist, restore the saved path

When province changes:

1. set selected province
2. clear second-level selection
3. clear third-level selection
4. if province has any `CITY` children, set path mode to `CITY_OPTIONAL_DISTRICT`
5. otherwise set path mode to `DIRECT_DISTRICT`
6. load `secondLevelOptions` from the province children
7. clear `thirdLevelOptions`

When second-level selection changes on normal path:

1. set second-level node
2. clear third-level selection
3. load district children into `thirdLevelOptions`
4. confirm becomes available immediately

When second-level selection changes on direct path:

1. set second-level node
2. clear third-level selection
3. keep `thirdLevelOptions` empty
4. confirm becomes available

When third-level selection changes:

1. set third-level node
2. keep confirm available

### Return Mechanism

Use `FragmentResult` to return confirmed `SelectedRegion` back to `AddTodoFragment`.

This is enough for the one-shot confirm flow and avoids unnecessary shared view model coupling.

## Edge Cases

### Province Has No Children

Treat as invalid dataset entry.

Expected behavior:

- item is non-selectable, or
- item shows an error such as `该地区数据不完整`

The picker must not crash.

### City Has No District Children

User can confirm after selecting the city. District remains optional because there is no lower-level data.

### Special Province-Level Unit Without City Layer

If the province-level unit has district children directly and no city layer:

- skip city step
- require second-level district selection
- persist that second-level node into `cityCode/cityName`

### Mixed Child Levels Under One Province

If malformed data contains both direct district and city children under one province:

- prefer the city path if any city child exists
- continue safely without crashing

### Saved Code Missing From Current Dataset

If edit restoration cannot find a saved code:

- do not crash
- continue showing saved `formattedAddress` in the todo form when available
- clear the broken restoration path inside the picker
- let the user choose again and overwrite on confirm

### Parent Switching Reset

Mandatory reset rules:

- changing province clears second-level and third-level state
- changing second-level clears third-level state

This prevents invalid cross-parent combinations.

## Integration Impact

The current todo models do not include region data. Implementation will need to extend:

- `Todo`
- `TodoEntity`
- Room schema and migration handling as appropriate for the project
- repository mapping
- save and update logic in `AddTodoViewModel`
- add/edit UI in `AddTodoFragment`

Suggested nullable persisted fields:

```kotlin
val provinceCode: String?
val provinceName: String?
val cityCode: String?
val cityName: String?
val districtCode: String?
val districtName: String?
val formattedAddress: String?
```

Nullability is needed for existing records and for todos that do not yet have region data.

## Testing Focus

Implementation should verify at least these scenarios:

1. province -> city confirm works
2. province -> city -> district confirm works
3. changing province resets lower levels
4. changing city resets district
5. direct province -> district path skips city and requires second-level selection
6. direct path persists second-level selection into `cityCode/cityName`
7. edit mode restores normal path correctly
8. edit mode restores direct path correctly
9. municipality name de-duplication works in `formattedAddress`
10. incomplete region data does not crash the picker

## Key Design Decision

The most important design decision is this:

When a province-level administrative unit has no city-level layer, the selected second-level district node is stored in `cityCode` and `cityName`, while `districtCode` and `districtName` remain empty.

That preserves the required output shape and avoids creating synthetic city-level data.
