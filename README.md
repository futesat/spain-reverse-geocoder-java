# 🇪🇸 Spain Reverse Geocoder

[![Build & Test](https://github.com/futesat/spain-reverse-geocoder-java/actions/workflows/build.yml/badge.svg)](https://github.com/futesat/spain-reverse-geocoder-java/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![No Dependencies](https://img.shields.io/badge/dependencies-none-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)

**Offline, zero-dependency reverse geocoding and municipality search for Spain** using exact municipality polygons and a quadtree spatial index.

Given coordinates → returns the **municipality**, **province**, **autonomous community**, and the **municipality polygon** (GeoJSON geometry).

---

## ✨ Features

| Feature | Description |
|---|---|
| **Reverse Geocoding** | Exact point-in-polygon lookup using a quadtree spatial index |
| **Municipality Search** | Find municipalities by name (exact or partial), accent & case insensitive |
| **Province + Name Search** | Filter by province (name or INE code) then search municipality |
| **Province Filtering** | Load only specific provinces to optimize memory |
| **Embedded Data** | ~92 MB GeoJSON with all 8,131 Spanish municipalities bundled in the JAR |
| **Library API** | Clean builder-pattern API for use as a dependency in other Java projects |
| **CLI Tool** | Command-line interface for `lookup` and `search` operations |
| **Geometry Output** | Returns the full municipality polygon as GeoJSON in results |
| **Low Precision Mode** | Option to use ~75MB GeoJSON (vs ~90MB) to save memory/space |
| **Zero Dependencies** | Pure Java 21 — no Maven, no Gradle, no external libraries |

---

## 🚀 Quick Start

### As a Library

```java
import com.futesat.spaingeo.SpainGeo;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

// 1. Build (loads embedded GeoJSON automatically)
SpainGeo geo = SpainGeo.builder().build();

// 2. Reverse geocode
ReverseGeocodeResult result = geo.reverse(40.4167, -3.70325);
System.out.println(result.municipality().name()); // → "Madrid"

// 3. Search by name
List<ReverseGeocodeResult> results = geo.searchByName("Córdoba");
```

### As a CLI Tool

```bash
# Reverse geocode
java -jar spain-reverse-geocoder.jar lookup --lat 40.4167 --lon -3.70325

# Search by name
java -jar spain-reverse-geocoder.jar search --name "Madrid"
```

---

## 📦 Build

Requires **JDK 21+**. No build tools needed.

```bash
./scripts/build.sh
```

Output:
- `build/spain-reverse-geocoder.jar` — self-contained JAR with embedded GeoJSON data

---

## 📖 Library API Reference

### Initialization

```java
// Default: loads all municipalities from embedded GeoJSON
SpainGeo geo = SpainGeo.builder().build();

// Load only specific provinces (saves memory)
SpainGeo geo = SpainGeo.builder()
    .provinces("28", "08", "46")  // Madrid, Barcelona, Valencia
    .build();

// Use your own GeoJSON file
SpainGeo geo = SpainGeo.builder()
    .geoJsonPath(Path.of("/path/to/municipalities.geojson"))
    .build();

// Use low-precision mode (saves ~15MB JAR space and memory)
SpainGeo geo = SpainGeo.builder()
    .lowPrecision(true)
    .build();
```

### Reverse Geocoding

```java
ReverseGeocodeResult result = geo.reverse(40.4167, -3.70325);

if (result != null) {
    result.municipality().id();    // "28079"
    result.municipality().name();  // "Madrid"
    result.province().id();        // "28"
    result.province().name();      // "Madrid"
    result.autonomousCommunity().id();    // "13"
    result.autonomousCommunity().name();  // "Madrid, Comunidad de"
    result.geometry().toJson();    // GeoJSON Polygon/MultiPolygon
}
```

### Municipality Search

All search methods are **accent-insensitive** and **case-insensitive**. Searching for `"cordoba"` will match `"Córdoba"`, `"CÓRDOBA"`, etc.

```java
// Exact name match
List<ReverseGeocodeResult> results = geo.searchByName("Córdoba");

// Partial/substring match
List<ReverseGeocodeResult> results = geo.searchByNameContains("madri");

// Filter by province + municipality name
List<ReverseGeocodeResult> results = geo.search("Madrid", "Getafe");
List<ReverseGeocodeResult> results = geo.search("28", "Getafe");  // by province code
```

### Province Codes

Province filtering uses **2-digit INE codes**. Some common ones:

| Code | Province | Code | Province |
|------|----------|------|----------|
| `01` | Álava | `28` | Madrid |
| `08` | Barcelona | `29` | Málaga |
| `11` | Cádiz | `33` | Asturias |
| `15` | A Coruña | `41` | Sevilla |
| `20` | Gipuzkoa | `46` | Valencia |
| `48` | Bizkaia | `50` | Zaragoza |

---

## 🖥️ CLI Reference

### `lookup` — Reverse Geocode

```bash
java -jar spain-reverse-geocoder.jar lookup \
  --lat 40.4167 \
  --lon -3.70325
```

Output:
```json
{
  "municipality": { "id": "28079", "name": "Madrid" },
  "province": { "id": "28", "name": "Madrid" },
  "autonomousCommunity": { "id": "13", "name": "Madrid, Comunidad de" },
  "geometry": {"type":"Polygon","coordinates":[...]}
}
```

### `search` — Find Municipalities by Name

```bash
# Exact match
java -jar spain-reverse-geocoder.jar search --name "Madrid"

# Partial match
java -jar spain-reverse-geocoder.jar search --name "madri" --partial

# Filter by province
java -jar spain-reverse-geocoder.jar search --province "Madrid" --name "Getafe"
```

### Common Options

| Option | Description |
|---|---|
| `--geojson <path>` | Path to custom GeoJSON file (default: embedded) |
| `--provinces <codes>` | Comma-separated province codes to load (e.g. `28,08`) |
| `--mapping <path>` | Path to property mapping JSON |
| `--catalog <path>` | Path to administrative catalog JSON |
| `--low-precision` | Use 4-decimal precision data (~75MB vs ~90MB) |

---

## 🏗️ Architecture

```
src/main/java/com/futesat/spaingeo/
├── SpainGeo.java                    # Public library API (builder pattern)
├── cli/
│   └── Main.java                    # CLI entry point (lookup + search)
├── core/
│   ├── SpainReverseGeocoder.java    # Reverse geocoding engine
│   ├── QuadtreeSpatialIndex.java    # Quadtree spatial index
│   ├── MunicipalityFeature.java     # Feature record (result + geometry)
│   ├── MunicipalityIndex.java       # Name search index
│   └── TextNormalizer.java          # Accent/case-insensitive normalization
├── geo/
│   ├── Geometry.java                # Geometry interface
│   ├── PolygonGeometry.java         # Polygon implementation
│   ├── MultiPolygonGeometry.java    # MultiPolygon implementation
│   ├── Ring.java                    # Ring (coordinate sequence)
│   ├── Coordinate.java              # (x, y) coordinate record
│   └── Envelope.java                # Bounding box
├── io/
│   ├── GeoJsonLoader.java           # GeoJSON parser with province filtering
│   ├── MiniJsonParser.java          # Zero-dependency JSON parser
│   ├── PropertyMapping.java         # GeoJSON property name mapping
│   ├── PropertyMappingLoader.java   # Mapping file loader
│   └── SpainCatalog.java            # Province/community catalog
└── model/
    ├── ReverseGeocodeResult.java    # Result record
    ├── AdminDivision.java           # (id, name) record
    └── JsonEscaper.java             # JSON string escaper
```

### How It Works

1. **Loading**: GeoJSON FeatureCollection is parsed, optionally filtered by province code
2. **Indexing**: Municipality polygons are inserted into a quadtree spatial index; names are indexed for search
3. **Reverse Geocoding**: The quadtree narrows candidates by bounding box, then exact point-in-polygon tests find the match
4. **Name Search**: Text is normalized (NFD decomposition removes accents, lowercased) for accent/case-insensitive matching

---

## 📊 Data Source

The embedded GeoJSON contains official municipal boundaries from:

**[Centro de Descargas CNIG](https://centrodedescargas.cnig.es/CentroDescargas/limites-municipales-provinciales-autonomicos)** — Límites municipales, provinciales y autonómicos (IGN/CNIG)

To use your own data, convert to GeoJSON with `ogr2ogr`:

```bash
ogr2ogr -f GeoJSON municipalities.geojson MUNICIPIOS.shp
```

### Property Detection

The loader auto-detects common property names:

| Field | Detected Names |
|---|---|
| Municipality ID | `municipalityId`, `municipality_id`, `id`, `CODIGO`, `CMUNI`, `CUMUN`, `INE_MUNI`, `CODMUN` |
| Municipality Name | `municipalityName`, `municipality_name`, `name`, `LITERAL`, `MUNICIPIO`, `NOMBRE`, `NMUNI` |
| Province ID | `provinceId`, `province_id`, `CPRO`, `CPROV`, `INE_PROV`, `CODPROV` |
| Province Name | `provinceName`, `province_name`, `PROVINCIA`, `NPRO`, `NPROV` |

Override with a custom mapping JSON file via `--mapping`.

---

## 🧪 Testing

```bash
./scripts/test.sh
```

The test suite covers:
- Reverse geocoding (positive + negative)
- Polygon with holes
- Province filtering
- Text normalization (accents, ñ, ü, ç)
- Name search (exact, partial, case-insensitive, accent-insensitive)
- Province + name search
- Geometry JSON serialization
- Catalog lookups
- JSON escaping
- Builder API

---

## 🔧 CI/CD

GitHub Actions automatically builds and tests on every push to `main` and on pull requests. The JAR is uploaded as a build artifact.

---

## 📝 Notes

- Coordinates use **WGS84** (longitude/latitude in decimal degrees)
- Points on polygon borders are treated as **inside**
- The algorithm handles both `Polygon` and `MultiPolygon` geometries
- Province/community data is inferred from the municipality code (first 2 digits = province INE code) when not present in properties

---

## 📄 License

MIT
