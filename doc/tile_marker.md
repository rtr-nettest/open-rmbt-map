# `RMBTMapServer/tiles/markers` request and response

Returns the measurements near a clicked map position, or a single measurement selected by
`open_test_uuid`. Clients use it to fill the marker popup / tooltip.

- **Method / path:** `POST {mapServerBase}/tiles/markers`
- **Content type:** `application/json`
- **Implementation:** `MarkerController` → `MarkerService.getMarkersForPoint()`

## Request body

```jsonc
{
  "language": "de",
  "open_test_uuid": null,
  "coords": { "x": 1823451.08, "y": 6003949.09, "z": 15, "size": 20 },
  "options": { "map_options": "mobile/download" },
  "filter": { "highlight": null, "period": 180, "operator": null, "provider": null, "technology": null },
  "capabilities": { "classification": { "count": 4 } }
}
```

| Field | Type | Notes |
|---|---|---|
| `language` | string | Language of the labels (`title`, `value`, `time_string`). Unsupported values fall back to the default language (`app.defaultLanguage`). |
| `open_test_uuid` | string \| null | Selects one test directly; `coords` and `filter` are then ignored. Plain UUID **without** the `O` prefix (an unparsable value is ignored). |
| `coords` | object | Clicked position, used when `open_test_uuid` is not set. |
| `coords.x`, `coords.y` | number | Position in EPSG:3857 (meters). Takes precedence over `lat`/`lon`. |
| `coords.lat`, `coords.lon` | number | Position in WGS84 degrees. `long` is accepted as an alias of `lon`. |
| `coords.z` | integer | Zoom level; required together with a position. |
| `coords.size` | integer | Search radius in pixels at zoom `z`; default 10 pixels. |
| `options.map_options` | string | Map option, e.g. `mobile/download`, `mobile/signal`, `wifi/upload`, `mobile/fences`, `mobile/technology`. Default `mobile/download`. `mobile/fences` and `mobile/technology` query **fences** (coverage/signal measurements), all others query **tests**. |
| `filter.highlight` | string \| null | Client UUID; markers of this client's own tests get `highlight: true` and are sorted first. |
| `filter.period` | integer \| null | Only tests from the last *n* days (1–2922). Applies to the start time of the test (`test.time`), also for fences. |
| `filter.operator` | string \| null | Mobile operator filter. |
| `filter.provider` | string \| null | Provider (ISP) filter. |
| `filter.technology` | string \| null | Technology filter. |
| `capabilities.classification.count` | integer | Number of classification levels the client supports (default 3). |

At most **5** markers are returned, newest first.

## Response body

```jsonc
{
  "measurements": [
    {
      "highlight": null,
      "time": 1790412561137,
      "time_string": "Sep 26, 2026, 10:49:21 AM",
      "lat": 1823451.0861174846,
      "lon": 6003949.091409557,
      "open_test_uuid": "O47bcc862-d126-4c82-8b68-b7cc046fe486",
      "measurement": [ { "title": "Download", "value": "140 Mbps", "classification": 4 } ],
      "measurement_result": { "download_kbit": 139500, "download_classification": 4, "...": "..." },
      "net": [ { "title": "Connection", "value": "4G (LTE)" } ],
      "network_info": { "network_type_label": "4G (LTE)", "provider_name": "T-Mobile AT (232-03)", "wifi_ssid": null, "roaming_type_label": "International" }
    }
  ]
}
```

### Marker object (`measurements[]`)

| Field | Type | Notes |
|---|---|---|
| `time` | integer (ms) | Measurement time, see [Time fields](#time-fields). |
| `time_string` | string | **Deprecated.** Measurement time as formatted text, see [Time fields](#time-fields). |
| `highlight` | boolean \| null | `true` if the test belongs to the client given in `filter.highlight`, otherwise `null`. |
| `lat`, `lon` | number | Marker position (for fences: the fence position). Units depend on the request, see note below. |
| `open_test_uuid` | string | Open test UUID with `O` prefix. For fences, the UUID of the coverage test the fence belongs to. |
| `measurement` | MetricItem[] | Localised results for display (download, upload, ping, signal). |
| `measurement_result` | object | Raw results, see below. |
| `net` | MetricItem[] | Localised network information for display. |
| `network_info` | object | Raw network information, see below. |

Note on `lat`/`lon`: the server returns WGS84 degrees only if the request was made with
`coords.lat`/`coords.lon`. With `coords.x`/`coords.y` or with `open_test_uuid` alone, the
fields hold **EPSG:3857 meters** despite their names: `lat` = x, `lon` = y.

### MetricItem (`measurement[]`, `net[]`)

| Field | Type | Notes |
|---|---|---|
| `title` | string | Localised label, e.g. `"Download"`, `"Signal (4G)"`, `"Mobile net"`. |
| `value` | string | Localised, formatted value incl. unit, e.g. `"140 Mbps"`, `"-85 dBm"`. |
| `classification` | integer | Classification 1 (worst) … `count` (best); omitted for `net` items. |

### `measurement_result`

| Field | Type | Notes |
|---|---|---|
| `download_kbit`, `upload_kbit` | integer \| null | Throughput in kbit/s. `null` for fences without a speed test. |
| `download_classification`, `upload_classification` | integer \| null | Classification of the throughput. |
| `ping_ms` | number \| null | Median ping in ms (fences: average ping of the fence). |
| `ping_classification` | integer \| null | Classification of the ping. |
| `signal_strength` | integer \| null | Signal strength in dBm (2G/3G/WLAN). |
| `lte_rsrp` | integer \| null | RSRP in dBm (4G/5G; fences: signal of the fence). |
| `signal_classification` | integer \| null | Classification of `signal_strength`, or of `lte_rsrp` if present. |

### `network_info`

| Field | Type | Notes |
|---|---|---|
| `network_type_label` | string | e.g. `"4G (LTE)"`, `"5G (NR)"`, `"WLAN"`. |
| `provider_name` | string \| null | Mobile network or provider name. |
| `wifi_ssid` | string \| null | Only for the client's own WLAN tests (`highlight`). |
| `roaming_type_label` | string | Only present for international roaming. |

## Time fields

`time` and `time_string` describe the same instant.

| Field | Format | Time zone |
|---|---|---|
| `time` | Unix epoch in **milliseconds** (e.g. `1790412561137` = 2026-09-26 08:49:21.137 UTC) | None; an absolute point in time. |
| `time_string` | Java `DateFormat` MEDIUM date + MEDIUM time in the request's `language`, e.g. `"Sep 26, 2026, 10:49:21 AM"` (en) | Time zone of the **measuring device** (`test.timezone`). |

**Which time is reported**

- **Tests:** `test.time`, the start of the test.
- **Fences** (`mobile/fences`, `mobile/technology`): `fences.fence_time`, the time of the
  individual fence (= start of the coverage test + `offset_ms`).

**Which time zone `time_string` uses**

`time_string` is always formatted in the time zone that the **measuring client** reported
to the control server, stored in `test.timezone`. For coverage/signal measurements this is
the `timezone` of the result submission (it overwrites the value from the registration).
The time zone of the client **requesting** the markers is never sent to the map server and
is not used. Example: a test made in Vienna in summer is shown as `10:49:21` to all
viewers, including a viewer in New York or one whose browser runs in UTC.

If `test.timezone` is empty or not a valid time zone ID (e.g. `CEST` or `+02:00` instead
of `Europe/Vienna`), `time_string` is formatted in **UTC**.

**`time_string` is deprecated.** It is still sent for existing clients, but new clients
should format `time` themselves, which shows the viewer's local time and language, e.g. in
JavaScript `new Date(marker.time).toLocaleString()`. The field will be removed once no
client uses it anymore.
