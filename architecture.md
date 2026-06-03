# RMBT Map Server — Architecture

> Companion documents: `architecture.md` in **open-rmbt-control** (the *write* side — records
> measurements) and **open-rmbt-statistics** (the *read/reporting* side). The Map Server is a
> third reader of the **same** PostgreSQL database, specialised for one thing: drawing maps.

---

## 1. What this server is

**RMBT** ("RTR Multithreaded Broadband Test") is the engine behind **RTR-Netztest**, the public
internet-quality measurement service of RTR-GmbH. Every measurement an end user runs is stored by
the **Control Srepeerver** as a `test` row with a geographic location.

The **Map Server** is the backend for the **interactive map** on netztest.at. It turns millions of
geolocated measurements into the visual layers you see on a slippy map:

1. **Map tiles** — 256×256 **PNG** images for a given `{zoom}/{x}/{y}`, in three flavours:
   - **points** — individual measurement points, coloured by value;
   - **heatmap** — density/heat of measurements;
   - **shapes** — administrative areas (e.g. municipalities) coloured by their aggregate value.
2. **Markers** — for a clicked location, the nearby individual measurements (the "what tests are
   here?" popup).
3. **Tile info** — aggregate statistics for an area / per operator (the popup numbers).

Crucially, like the Statistics Server:

- **It is read-only.** It does **not** own or migrate the schema (no Flyway) and never writes
  measurements. It reads the database the Control Server populates.
- **It is a public API with no authentication.** There is **no Spring Security** — tiles are
  public. The only inbound policy is **CORS**. (So the SQL-safety discipline in §6 is essential.)
- **It is render- and cache-heavy.** Each tile is a PostGIS spatial aggregation turned into a
  Java2D image; a heavily-used **Redis tile cache** is what keeps it fast.

Where this server sits among the RMBT components:

| Server | Repo | Role |
|--------|------|------|
| Control Server | `open-rmbt-control` | Writes: registers tests, stores results. |
| Statistics Server | `open-rmbt-statistics` | Reads: search, statistics, exports. |
| **Map Server** | `open-rmbt-map` (this repo) | **Reads: renders map tiles / markers / area stats.** |
| QoS Server | `open-rmbt-qos` | Runs active QoS probes. |
| Measurement Server(s) | (separate) | The peers clients exchange bytes with. |

---

## 2. Technology stack

- **Language / runtime:** Java 17.
- **Framework:** Spring Boot 3.x (Spring MVC, Spring Data JPA, Spring Cache). **No Spring
  Security.**
- **Packaging / hosting:** a **WAR** (`<packaging>war</packaging>`) deployed into external
  **Tomcat 10**. `MapServerConfiguration` is the `@SpringBootApplication` and extends
  `SpringBootServletInitializer`; it also has a `main()` for IDE run/debug.
- **Database:** PostgreSQL + **PostGIS** (read access to the shared RMBT DB). Hibernate 6 / Spring
  Data JPA for the one mapped entity; **native spatial SQL** for the tile/marker/info queries.
  **hibernate-spatial** + **postgis-jdbc**. No Flyway.
- **Caching:** **Redis** (`spring-data-redis` + Jedis; Lettuce excluded) behind Spring Cache. Used
  **manually** as a tile cache with a stale-while-revalidate strategy (see §7), not via
  `@Cacheable`.
- **Rendering:** plain **Java2D** (`java.awt` `BufferedImage`) — tiles are drawn in memory and
  serialised to PNG bytes. There is no templating/HTML here; the output is images (or JSON).
- **API docs:** springdoc-openapi (Swagger UI). Logging via logstash-logback-encoder. Utilities:
  Guava-style helpers, commons-lang3/io. **No Spring Security, no Flyway, no MapStruct.**

One-line mental model: **a read-only PostGIS-to-PNG tile renderer with an aggressive Redis tile
cache.**

---

## 3. High-level architecture

```
                         HTTP  (image/png  or  application/json)
                                       │
                ┌───────────────────────────────────────────────┐
                │  Servlet filters: ApiLoggingFilter            │   (no security chain)
                │  CORS (WebMvcConfig)                          │
                └───────────────────────────────────────────────┘
                                       │
              ┌───────────────────────────────────────────────────┐
              │  Controllers (controller/)                        │  bind URL/params → request DTO
              │  Tiles · Marker · TilesInfo · ApplicationVersion  │
              └───────────────────────────────────────────────────┘
                                       │
      ┌─────────────────────────────────────────────────────────────────┐
      │  Services (service/)                                            │
      │   TileGenerationService (abstract base: cache + render)         │
      │     ├── PointTileService    ├── ShapeTileService                │
      │     └── HeatmapTileService                                      │
      │   MarkerService · InfoService · FiltersService · …              │
      └─────────────────────────────────────────────────────────────────┘
           │                         │                        │
   ┌─────────────────┐     ┌─────────────────────┐    ┌───────────────────┐
   │ MapServerOptions│     │ native PostGIS SQL  │    │ Redis tile cache  │
   │ (whitelist +    │     │ (aggregate points   │    │ (CachedTile,      │
   │  bound filters) │     │  into a tile)       │    │  stale-while-     │
   │                 │     │                     │    │  revalidate)      │
   └─────────────────┘     └─────────────────────┘    └───────────────────┘
           │                         │                        │
           └──────────► PostgreSQL + PostGIS (read)           Redis
                                       │
                              Java2D render → PNG bytes
```

Cross-cutting concerns:

- **`filter/ApiLoggingFilter`** — per-request logging (method, path, params, body, headers;
  binary-safe so it doesn't dump PNG bytes).
- **`WebMvcConfig`** — CORS mappings + a CORS `Filter`. (No security chain.)
- **`RedisConfig`** — the tile cache manager / serializer.

---

## 4. Slippy-map tiles in 60 seconds (why the endpoints look like they do)

Web maps (Leaflet, Google Maps, OpenLayers) render the world as a pyramid of 256×256 image
**tiles**. At zoom `z` the world is a `2^z × 2^z` grid; a tile is addressed by `{z}/{x}/{y}`. As you
pan/zoom, the browser requests exactly the tiles it needs. So **one map view = many parallel tile
requests**, and the same tiles are requested over and over by many users — which is why caching
dominates the design.

Coordinates are **Web Mercator (EPSG:3857, a.k.a. "900913"/Google)**. The server converts a
`{z}/{x}/{y}` tile address into a bounding box in that projection, runs a PostGIS query for the
measurements inside that box, and paints them.

**Endpoints** (paths in `constant/URIConstants`):

- `GET /tiles/{type}/{zoom}/{x}/{y}.png` — the main tile endpoint. `type` ∈ `points|shapes|heatmap`
  (`TilesController`). Returns `image/png`.
- `GET /tiles/{type}?path=z/x/y&…` — same, with the tile address packed into a `path` param (used
  by the iOS client).
- `POST /tiles/markers` — markers near a location (`MarkerController`).
- `POST /tiles/info`, `POST|GET /v2/tiles/info` — aggregate info for an area / operators
  (`TilesInfoController`).
- `GET /version` — build/version info (`ApplicationVersionController`).

The big query string on the tile endpoints (`map_options`, `statistical_method`, `period`, `age`,
`operator`, `provider`, `technology`, `transparency`, `point_diameter`, `highlight`, …) is the
**filter/style** the UI is currently showing — see §6.

---

## 5. The tile pipeline (the core path — read this)

All three tile types share one base class, **`service/TileGenerationService`** (abstract). The
concrete services (`PointTileService`, `ShapeTileService`, `HeatmapTileService`) provide the
type-specific SQL and drawing; the base provides the **cache + orchestration**. The flow of a
single `GET /tiles/{type}/{z}/{x}/{y}.png`:

1. **Controller → request DTO.** `TilesController.getTiles(...)` collects the many query params into
   a `TilesRequest`, picks the type (`points`/`shapes`/`heatmap`), wraps the address in a
   `TileParameters` (e.g. `PointTileParameters` carrying the `Path(zoom,x,y)`), and calls the
   matching service's `getTile(params)`.
2. **Cache lookup** (`TileGenerationService.getTile`). A deterministic **cache key** is computed as
   the **SHA-1 of the tile parameters' string form** (so identical tile+filter requests share a
   cache entry). It looks up the `tile_cache` Redis cache for a `CachedTile`:
   - **Fresh hit** → return the cached PNG bytes immediately.
   - **Stale hit** (older than `TILE_CACHE_STALE` = 1 h) → return the cached bytes **now** and
     trigger an **asynchronous regeneration** (via a `TaskExecutor`) so the next request is fresh.
     This **stale-while-revalidate** behaviour keeps latency low while data stays reasonably
     current.
   - **Miss** → generate synchronously.
3. **Generate** (subclass). The concrete service:
   - builds a **PostGIS query** for the tile's bounding box, applying the selected map option and
     filters (§6), and aggregates the measurements (e.g. snap points to a sub-grid, compute a
     percentile/`statistical_method`, count, etc.);
   - draws the result into a **Java2D `BufferedImage`** (points as coloured dots, shapes as filled
     polygons, heatmap as a density field), honouring style params (transparency, point diameter,
     colour/fill toggles, highlight);
   - encodes the image to **PNG bytes**.
4. **Store & return.** The bytes are wrapped in a `CachedTile` (with a creation timestamp), written
   to Redis, and returned with `Content-Type: image/png`. Empty tiles (no data) are handled via
   pre-generated blank images so they're cheap.

Marker and info requests follow a simpler version of the same idea (`MarkerService`,
`InfoService`): a PostGIS query for the area, mapped to a JSON response DTO — no image rendering.

---

## 6. Map options, filters & the SQL-safety boundary

Because tile queries embed **client-supplied** selection into SQL and there is **no auth**,
`util/MapServerOptions` is the security-critical class. It defines, as server-side constants:

- **`map_options` → column mapping.** The `map_options` request value (e.g. `mobile/download`,
  `wifi/upload`, `browser/ping`, `all/signal`) is a **key into a fixed map** of `MapOption`
  objects, each of which names the actual DB column/measure to render (`speed_download`,
  `speed_upload`, `ping_median`, `signal_strength`, …) plus its classification/colour scale. The
  client never supplies a column name — only a whitelisted key.
- **Fixed base filters.** Every tile query always includes hard-coded `SQLFilter`s:
  `t.deleted = false AND t.implausible = false AND t.status = 'FINISHED'`, plus an accuracy filter
  (`t.geo_accuracy < 2000`). Bad/old/implausible data is excluded server-side.
- **Parameterised optional filters.** `operator`, `provider`, `technology`, `period`, `age`,
  `user_server_selection` are handled by `MapFilter` entries whose `getFilter(input)` returns an
  `SQLFilter` built with **`?` placeholders**; the user value is **bound** at execution, never
  concatenated. (Comparisons and column names come from the server; only values come from the
  user.)

> **Rule for any change here:** add new selectable metrics to the `MapServerOptions` whitelist and
> new filters as bound-parameter `SQLFilter`s. Never interpolate a user string into the SQL text.
> This whitelist *is* the input validation for the whole server.

---

## 7. The tile cache (Redis)

Tiles are expensive to compute and massively re-requested, so caching is first-class:

- **`RedisConfig`** wires a Jedis connection and a `RedisCacheManager`. The `tile_cache` cache
  stores `CachedTile` objects (the PNG bytes + a creation time) serialised as JSON.
- **TTL / staleness constants** (`constant/Constants`):
  - `TILE_CACHE_EXPIRE = 24 h` — hard Redis TTL for a cached tile.
  - `TILE_CACHE_STALE = 1 h` — older than this → still served, but regenerated in the background
    (stale-while-revalidate).
  - `TILE_SHORT_CACHE_EXPIRE = 5 min` — a shorter freshness window used for recent-data tiles.
- **Manual cache-aside, not `@Cacheable`.** The logic lives in `TileGenerationService.getTile`
  (SHA-1 key, freshness/staleness checks, async regeneration) because the policy (serve-stale +
  background refresh, blank-tile shortcuts, per-request `no_cache`) is richer than a declarative
  annotation can express.
- Redis is a **performance layer, not a source of truth**: if it's down, tiles are computed on
  demand (slower), not wrong.

---

## 8. Package-by-package guide

Everything is under `at.rtr.rmbt.map`.

### `controller/`
Thin HTTP adapters returning PNG bytes or JSON:
- **`TilesController`** — the `/tiles/{type}/…` endpoints; routes by tile type to the tile services.
- **`MarkerController`** — `POST /tiles/markers`.
- **`TilesInfoController`** — `/tiles/info` and `/v2/tiles/info`.
- **`ApplicationVersionController`** — `/version`.

### `service/`
The work. **`TileGenerationService`** is the abstract base (cache + orchestration + Java2D
helpers). Concrete renderers: **`PointTileService`**, **`ShapeTileService`**,
**`HeatmapTileService`**. Plus **`MarkerService`**, **`InfoService`** (area/operator aggregates),
**`FiltersService`** (available map filters/options for the UI), **`ApplicationVersionService`**.
(There is no separate `impl/` subpackage here — services are concrete classes.)

### `util/`
- **`MapServerOptions`** — the map-option whitelist and SQL filter definitions (see §6); the most
  important class to understand for safety and for adding metrics.
- **`GeoCalc`** / **`TileParameters`** — tile math: convert `{zoom,x,y}` to Web-Mercator bounding
  boxes, sub-grid sizing, the `TileParameters` hierarchy (point/shape/heatmap variants) used as
  both query input and cache key.
- **`Classification`** / **`FormatUtils`** / **`HelperFunctions`** — colour/quality classification,
  value formatting, misc helpers.

### `model/`
Mostly **query-result holders** (not JPA entities): `TilesQueryResult`, `ShapeTilesQueryResult`,
`MarkerQueryResult`, `TilesInfoOperatorResults`, and **`CachedTile`** (the Redis-cached tile). The
one real JPA `@Entity` is **`Settings`** (the `settings` table — system uuid, version, map config).

### `dto/`
The wire contract: `TilesRequest`, `MarkerRequest` / `MarkerResponse`, `TilesInfoRequest` /
`TilesInfoResponse`, `MapFiltersResponse`, `CapabilitiesRequest`, `ApplicationVersionResponse`.

### `repository/`
A single Spring Data repository, **`SettingsRepository`** (reads the `settings` entity). All the
heavy spatial queries are **native SQL built inside the services**, not derived repositories.

### `constant/`
**`URIConstants`** (endpoint paths) and **`Constants`** (tile types `POINT|HEATMAP|SHAPE`, cache
name `tile_cache`, the TTL constants, formatting templates).

### `filter/`
**`ApiLoggingFilter`** — per-request logging, the same design used across the RMBT servers
(request id in MDC, doesn't log binary/PNG bodies, friendly on client disconnects).

### Top-level config classes (`MapServerConfiguration`, `RedisConfig`, `WebMvcConfig`)
The application class, the Redis cache wiring, and the CORS/MVC config respectively (these sit
directly in `at.rtr.rmbt.map`, not in a `config` subpackage).

---

## 9. Persistence (read-only over a shared database)

- The server reads the **same PostgreSQL+PostGIS database** the Control Server writes, treating it
  as **read-only**. There is **no Flyway** and no schema ownership here.
- Almost all data access is **native, spatial SQL** assembled in the tile/marker/info services:
  PostGIS `ST_*` functions clip to the tile's bounding box (`ST_MakeBox2D` / `ST_SetSRID`),
  `ST_SnapToGrid` aggregates points into a sub-grid, and `percentile_disc(...)` computes the
  statistical method. The geometry/SRID work is what makes this a "map" server rather than a
  generic reporting one.
- The only JPA-mapped table is `settings`. The DB role needs **`SELECT`** only — keep it that way.

---

## 10. Cross-cutting concerns

### No authentication — CORS only
There is no Spring Security dependency; inbound requests are gated only by **CORS** (`WebMvcConfig`,
plus an explicit CORS `Filter`). Treat every endpoint as publicly reachable — which is why the
`MapServerOptions` whitelist (§6) is the real input-validation layer.

### Logging (`logback.xml`)
The Map Server uses a **profile-independent conditional logback**: it reads `LOG_HOST`/`LOG_PORT`
natively (env var / system property / `${…:-}` default), and an `<if condition>` block enables the
**Logstash** appender only when `LOG_HOST` is set (console limited to ERROR, Logstash gets INFO,
`app_name = map-service`); otherwise it logs INFO to the console. Per-request logging is
`ApiLoggingFilter`. *(Note: this differs from the Control/Statistics servers, which apply logging at
runtime via a `LoggingConfigurer`; the Map Server keeps the simpler in-XML conditional.)*

### Error handling
Controllers validate inputs (e.g. a malformed `path` → `400 Bad Request`) and otherwise let Spring
map exceptions. Keep error responses cheap and avoid leaking stack traces to clients.

### Docs & monitoring
**springdoc** publishes the OpenAPI/Swagger UI. (Unlike Control/Statistics, JavaMelody is not
bundled here.)

### Async
A `TaskExecutor` drives background tile regeneration for the stale-while-revalidate cache (§7); be
mindful that regeneration runs off the request thread.

---

## 11. Configuration & deployment

- **Profiles** (`application.yml`): a default block plus a `prod` document
  (`spring.config.activate.on-profile`); activate via `spring.profiles.active=prod` in Tomcat's
  `catalina.properties`.
- **Externalised settings** (env / Tomcat `context.xml` `<Parameter>`):
  - DB: `MAP_DB_HOST` / `_PORT` / `_NAME` / `_USER` / `_PASSWORD`.
  - Redis: `MAP_REDIS_HOST` / `MAP_REDIS_PORT`.
  - CORS origin; logging (`LOG_HOST`, `LOG_PORT`, `LOGGING_HOST`).
- **External runtime dependencies:** a reachable PostgreSQL+PostGIS (read) and a Redis instance.
- **Build / deploy:** `mvn clean package` → WAR → Tomcat 10 (`webapps/RMBTMapServer.war`),
  Java 17.
- **Local run/debug:** run `MapServerConfiguration.main()`; point it at a local DB (with geolocated
  data) and a local Redis. Hitting
  `/tiles/points/12/2232/1428.png?map_options=mobile/download&period=180` is a good first request.

---

## 12. Conventions you should follow

- **SQL safety is the prime directive** (§6): metrics come from the `MapServerOptions` whitelist;
  filter values are bound parameters; never string-concatenate user input into SQL. There is no
  auth to fall back on.
- **Read-only mindset:** don't write the schema, don't add Flyway, keep DB privileges at `SELECT`.
- **Cache-friendliness:** anything that affects a tile's pixels must be part of the
  `TileParameters` (and therefore the SHA-1 cache key), or you'll serve stale/incorrect tiles from
  cache. Conversely, don't put request-identity noise into the key or you'll destroy the hit rate.
- **Keep rendering off the hot path when you can:** prefer cache hits / blank-tile shortcuts;
  expensive work belongs in the (async) regeneration path.
- **DTOs out, not entities/result holders:** map query results to `dto/*` responses for JSON
  endpoints.

---

## 13. Where to start reading (suggested path)

1. **`constant/URIConstants`** + **`constant/Constants`** — endpoints and cache/tile constants.
2. **`controller/TilesController`** — see how a tile request is parsed and routed by type.
3. **`service/TileGenerationService`** — the cache + orchestration core (cache key, freshness,
   stale-while-revalidate, render hand-off).
4. **`service/PointTileService`** (then `Shape`/`Heatmap`) — the PostGIS query + Java2D drawing for
   one concrete type.
5. **`util/MapServerOptions`** — *the* class for map options, filters, and SQL safety.
6. **`util/GeoCalc` / `util/TileParameters`** — the tile-coordinate math underpinning the queries
   and the cache key.
7. **`RedisConfig`** + **`model/CachedTile`** — how tiles are cached.

---

## 14. Glossary

- **Tile** — a 256×256 PNG covering one `{zoom}/{x}/{y}` cell of the Web-Mercator grid.
- **Slippy map** — the pannable/zoomable web map (Leaflet/OpenLayers) that requests tiles as you
  move.
- **EPSG:3857 / 900913 / Web Mercator** — the projection tiles are computed in.
- **map_options** — the whitelisted key (e.g. `mobile/download`) selecting which metric to render.
- **statistical_method** — the percentile used to aggregate measurements within a tile cell.
- **points / heatmap / shapes** — the three tile renderings (individual points / density /
  area polygons).
- **Markers** — individual measurements near a clicked location.
- **Tile info** — aggregate (per-operator) statistics for an area, shown in popups.
- **CachedTile** — the Redis-stored tile (PNG bytes + creation time) used by the stale-while-
  revalidate cache.

---

*This describes the structure and intent of the code as it stands. The code is the source of
truth — start from the reading path in §13 and follow the calls. For the write side and the
reporting side of the same database, see `architecture.md` in `open-rmbt-control` and
`open-rmbt-statistics`.*
