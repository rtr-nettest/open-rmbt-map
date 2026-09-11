# `RMBTMapServer/v2/tiles/info` response

Reference for the **map filter/options** payload returned by the map server and
consumed by the web page, the iOS app and the Android app. It tells a client which filter groups
to render, which option is selected by default, which tile layers/params to request,
and how to draw the heatmap legend.

- **Method / path:** `POST {mapServerBase}/v2/tiles/info`
- **Content type:** `application/json`
## Request body

A small identification object; the server keys the response language off `language`.

| Field | Type | Notes |
|---|---|---|
| `language` | string | UI language for `title`/`summary` (e.g. `"en"`, `"de"`). |
| `name` | string | Client name, e.g. `"RTR-Netztest"`. |
| `type` | string | Client type, e.g. `"DESKTOP"`, `"MOBILE"`. |
| `version_code` | string | Client version code. |
| `version_name` | string | Client version name. |
| `open_test_uuid` | string \| null | Optional; used when info is requested in the context of a specific test. |

## Response — top level

```jsonc
{
  "map_filters": [ /* array of Filter objects, in display order */ ]
}
```

The only top-level key is **`map_filters`**: an ordered array of **filter groups**.
The reference response currently contains 8 groups: *Map type, Technology, Operator
(operator), Operator (provider), Map appearance, Overlay type, Period, Statistics*.

## Filter object

Each entry of `map_filters` describes one filter group (a selectable list).

| Field | Type | Req. | Description |
|---|---|---|---|
| `title` | string | yes | Localised group heading (e.g. `"Map type"`). Not unique — see [Operator ×2](#two-operator-groups). |
| `default` | boolean | yes | Group-level default flag (option-level `default` marks the selected entry). |
| `options` | Option[] | yes | The selectable entries (may nest, see below). |
| `icon` | string \| null | no | UI icon key, e.g. `MAP_TYPE`, `MAP_FILTER_TECHNOLOGY`, `MAP_FILTER_CARRIER`, `MAP_APPEARANCE`, `OVERLAY_TYPE`, `MAP_FILTER_PERIOD`. **`null`/absent for the Statistics group** (clients default it to a statistics icon). |
| `depends_on` | object | no | Visibility condition; the group is only shown when it holds. Currently only `{ "map_type_is_mobile": boolean }`. |
| `functions` | Function[] | no | Group-level actions applied whenever this group's selection changes (e.g. `drop_param`). |

## Option object

An entry inside `options`. Options can **nest**: the *Map type* group has two levels
(Mobile / WLAN / Browser / All → Download / Upload / Ping / Signal).

| Field | Type | Req. | Description |
|---|---|---|---|
| `title` | string | yes | Localised option label. |
| `default` | boolean | yes | `true` on the option selected initially in its group/level. |
| `summary` | string | no | Longer localised description (may be `""`). |
| `options` | Option[] | no | Child options (only used by *Map type*). |
| `params` | Params | no | Query params to apply to tile/marker requests when selected (see below). |
| `functions` | Function[] | no | Actions to run when selected (see below). |
| `heatmap` | ColorStop[] | no | Legend gradient for this option (present on the leaf *Map type* options). |

Leaf options carry **either** `params` (Map type, Technology, Operator, Period,
Statistics) **or** `functions` (Map appearance, Overlay type) — Map type leaf options
carry `params` **and** `heatmap`.

### `params`

Merged into the tile/marker request query string. All keys are optional; a given
option sets only the ones relevant to it.

| Key | Type | Used by | Meaning |
|---|---|---|---|
| `map_options` | string | Map type | Tile dataset, `"{group}/{metric}"`, e.g. `mobile/download`, `all/ping`, `mobile/signal`. |
| `overlay_type` | string | Map type | Suggested overlay for the dataset: `heatmap` (mobile/wifi) or `shapes` (browser/all). |
| `technology` | string | Technology | Technology filter code; `""` = all, e.g. `"345"` = 3G/4G/5G. |
| `operator` | number \| string | Operator (home network) | Operator id; `""` = all. |
| `provider` | number \| string | Operator (as-seen provider) | Provider id; `""` = all. |
| `period` | number | Period | Look-back window in days (e.g. `1`, `30`, `365`). |
| `statistical_method` | number | Statistics | Percentile as a fraction, e.g. `0.8` = 80th percentile. |
| `map_type_is_mobile` | boolean | Map type (top level) | **UI-only** flag driving `depends_on`; removed before hitting the tile server via the `drop_param` function. |

### `functions`

Client-side actions. Each is `{ "func_name": string, "func_params": {...} }`.

| `func_name` | Where | `func_params` | Effect |
|---|---|---|---|
| `drop_param` | Map type (group-level) | `{ key }` | Remove a param (here `map_type_is_mobile`) before building tile URLs — it only drives `depends_on`, not the server. |
| `change_appearance` | Map appearance options | `{ type: "normal" \| "sat" }` | Switch the base map appearance. |
| `set_overlay` | Overlay type options | `{ type, path?, z_index, tile_size }` | Set the active tile overlay. |
| `add_alt_overlay` | Overlay type → *Automatic* | `{ type, path, z_index, tile_size }` | Register an additional overlay so the client can switch automatically (e.g. heatmap ↔ points by zoom). |

`func_params` fields (all optional unless noted):

| Field | Type | Description |
|---|---|---|
| `type` | string | Overlay/appearance kind: `heatmap`, `points`, `shapes`, `automatic`, `normal`, `sat`. |
| `path` | string | Tile endpoint for the overlay, e.g. `/RMBTMapServer/tiles/heatmap`, `/tiles/points`, `/tiles/shapes`. |
| `z_index` | number | Stacking order of the overlay. |
| `tile_size` | number | Tile size in px (`256` for heatmap/points, `512` for shapes). |
| `key` | string | Param name for `drop_param`. |

### `heatmap` (legend)

An ordered gradient (currently 9 stops per metric). Captions label every **other**
stop; the in-between stops have `caption: ""` so clients can render a smooth bar with
sparse tick labels. Direction is worst→best in the response order.

```jsonc
"heatmap": [
  { "color": "#950000", "caption": "1" },   // labelled
  { "color": "#ab0000", "caption": "" },     // midpoint tick
  { "color": "#e68a00", "caption": "5" },
  ...
]
```

| Field | Type | Description |
|---|---|---|
| `color` | string | Hex colour of the stop. |
| `caption` | string | Value label (metric-dependent: Mbit/s, ms, dBm); `""` for unlabelled midpoints. |

### `depends_on`

Conditional visibility. Present on *Technology* and both *Operator* groups:

```jsonc
"depends_on": { "map_type_is_mobile": true }
```

These groups are only meaningful for the mobile map type, so the client hides them
unless the currently selected Map type option set `map_type_is_mobile: true`.

## The 8 filter groups currently returned

| # | `title` | `icon` | `depends_on` | Selection sets | Notes |
|---|---|---|---|---|---|
| 0 | Map type | `MAP_TYPE` | — | `map_options`, `overlay_type`, `map_type_is_mobile`; group fn `drop_param` | 2-level: Mobile/WLAN (App)/Browser/All → Download/Upload/Ping/Signal; leaf options carry `heatmap`. |
| 1 | Technology | `MAP_FILTER_TECHNOLOGY` | `map_type_is_mobile:true` | `technology` | e.g. all / 3G+ / 4G+ / 5G. |
| 2 | Operator | `MAP_FILTER_CARRIER` | `map_type_is_mobile:true` | `operator` | Home-network operator (~9 options). |
| 3 | Operator | `MAP_FILTER_CARRIER` | `map_type_is_mobile:true` | `provider` | As-seen provider incl. MVNOs (~94 options). |
| 4 | Map appearance | `MAP_APPEARANCE` | — | fn `change_appearance` | Normal / Satellite. |
| 5 | Overlay type | `OVERLAY_TYPE` | — | fn `set_overlay` (+ `add_alt_overlay`) | Automatic / Heatmap / Points / Communities(shapes). |
| 6 | Period | `MAP_FILTER_PERIOD` | — | `period` | 1 day … all. |
| 7 | Statistics | `null` | — | `statistical_method` | Percentile (e.g. 0.8). |

### Two "Operator" groups

Groups 2 and 3 share `title: "Operator"` and `icon: "MAP_FILTER_CARRIER"`; they are
distinguished only by the **param key** their options set — `operator` (home network)
vs `provider` (provider as observed on the SIM/roaming). Clients must not key filter
groups by `title`/`icon` alone.

## Consumer field coverage

- **Web** (`IMapFilter`, `map.service.ts`): consumes `title`, `summary`, `options`,
  `params.*`, `heatmap`, `functions`, `depends_on`, `default`, `icon`.
- **iOS** (`RMBTMapServer.swift`, `MapOptionResponse`): consumes the same tree.
- **Android** (`MapFilterItemV2` in `MapResponses.kt`): models `options`, `title`,
  `icon`, `depends_on`, `functions`, `params` — but **does not model `heatmap`**
  (no in-app legend from this payload).

## Minimal example (trimmed)

```jsonc
{
  "map_filters": [
    {
      "title": "Map type",
      "default": false,
      "icon": "MAP_TYPE",
      "options": [
        {
          "title": "Mobile",
          "default": false,
          "params": { "map_type_is_mobile": true },
          "options": [
            {
              "title": "Download",
              "default": true,
              "summary": "Map shows download speed",
              "params": { "map_options": "mobile/download", "overlay_type": "heatmap" },
              "heatmap": [
                { "color": "#950000", "caption": "1" },
                { "color": "#ab0000", "caption": "" },
                { "color": "#264d00", "caption": "1000" }
              ]
            }
          ]
        }
      ],
      "functions": [
        { "func_name": "drop_param", "func_params": { "key": "map_type_is_mobile" } }
      ]
    },
    {
      "title": "Technology",
      "default": false,
      "icon": "MAP_FILTER_TECHNOLOGY",
      "depends_on": { "map_type_is_mobile": true },
      "options": [
        { "title": "2G/3G/4G/5G", "default": true, "summary": "All mobile technologies", "params": { "technology": "" } },
        { "title": "3G/4G/5G", "default": false, "params": { "technology": "345" } }
      ]
    },
    {
      "title": "Overlay type",
      "default": false,
      "icon": "OVERLAY_TYPE",
      "options": [
        {
          "title": "Heatmap",
          "default": false,
          "summary": "Map shows tests as heatmap",
          "functions": [
            { "func_name": "set_overlay", "func_params": {
                "path": "/RMBTMapServer/tiles/heatmap", "z_index": 100000000, "tile_size": 256, "type": "heatmap" } }
          ]
        }
      ]
    }
  ]
}
```

