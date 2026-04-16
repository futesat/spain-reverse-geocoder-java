package com.futesat.spaingeo;

import com.futesat.spaingeo.core.MunicipalityFeature;
import com.futesat.spaingeo.core.MunicipalityIndex;
import com.futesat.spaingeo.core.SpainReverseGeocoder;
import com.futesat.spaingeo.core.TextNormalizer;
import com.futesat.spaingeo.io.GeoJsonLoader;
import com.futesat.spaingeo.io.PropertyMapping;
import com.futesat.spaingeo.io.SpainCatalog;
import com.futesat.spaingeo.model.AdminDivision;
import com.futesat.spaingeo.model.JsonEscaper;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SelfTest {
    private static int passed = 0;
    private static int failed = 0;

    private SelfTest() {
    }

    public static void main(String[] args) {
        System.out.println("=== Spain Reverse Geocoder Test Suite ===\n");

        // Core: reverse geocoding
        section("Reverse Geocoding");
        testReverseLookupMadrid();
        testReverseLookupBarcelona();
        testReverseLookupOutside();

        // Core: polygon with hole
        section("Polygon with Hole");
        testPolygonHoleInside();
        testPolygonHoleOutside();

        // Province filtering
        section("Province Filtering");
        testProvinceFilter();
        testProvinceFilterExclusion();

        // TextNormalizer
        section("Text Normalizer");
        testNormalizerBasic();
        testNormalizerAccents();
        testNormalizerNull();
        testNormalizerSpecialChars();

        // Name search - exact
        section("Name Search (Exact)");
        testSearchExactName();
        testSearchExactNameCaseInsensitive();
        testSearchExactNameAccentInsensitive();
        testSearchExactNameNotFound();

        // Name search - partial
        section("Name Search (Partial)");
        testSearchPartialName();
        testSearchPartialNameAccentInsensitive();

        // Name search - province+name
        section("Search by Province + Name");
        testSearchByProvinceAndName();
        testSearchByProvinceIdAndName();

        // Geometry JSON serialization
        section("Geometry JSON Serialization");
        testGeometryJsonContainsType();
        testGeometryJsonContainsCoordinates();

        // SpainCatalog
        section("Spain Catalog");
        testCatalogProvinceName();
        testCatalogCommunityForProvince();

        // JsonEscaper
        section("JSON Escaper");
        testEscaperBasic();
        testEscaperSpecialChars();
        testEscaperNull();

        // SpainGeo builder
        section("SpainGeo Builder API");
        testBuilderWithExternalFile();
        testBuilderSize();

        // Administrative Listing
        section("Administrative Listing");
        testListCommunities();
        testListProvinces();
        testListMunicipalitiesByProvince();
        testListMunicipalitiesByCommunity();
        testResultToJsonWithoutGeometry();

        // Summary
        System.out.println("\n=== Results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println();
        if (failed > 0) {
            System.err.println("FAILURE: " + failed + " test(s) failed.");
            System.exit(1);
        }
        System.out.println("All tests passed.");
    }

    // ─── Helpers ─────────────────────────────────────────────

    private static void section(String name) {
        System.out.println("--- " + name + " ---");
    }

    private static SpainReverseGeocoder loadGeocoder() {
        return new SpainReverseGeocoder(loadFeatures());
    }

    private static List<MunicipalityFeature> loadFeatures() {
        return GeoJsonLoader.load(
                Path.of("src/test/resources/sample_municipalities.geojson"),
                PropertyMapping.defaultMapping(),
                SpainCatalog.loadDefault()
        );
    }

    private static MunicipalityIndex loadIndex() {
        return new MunicipalityIndex(loadFeatures());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null && actual == null) {
            pass(label);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            pass(label);
        } else {
            fail(label, "expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (condition) {
            pass(label);
        } else {
            fail(label, "expected true");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (!condition) {
            pass(label);
        } else {
            fail(label, "expected false");
        }
    }

    private static void assertNull(Object value, String label) {
        if (value == null) {
            pass(label);
        } else {
            fail(label, "expected null, got " + value);
        }
    }

    private static void assertNotNull(Object value, String label) {
        if (value != null) {
            pass(label);
        } else {
            fail(label, "expected non-null");
        }
    }

    private static void pass(String label) {
        System.out.println("  ✓ " + label);
        passed++;
    }

    private static void fail(String label, String detail) {
        System.out.println("  ✗ " + label + " — " + detail);
        failed++;
    }

    // ─── Reverse Geocoding Tests ─────────────────────────────

    private static void testReverseLookupMadrid() {
        SpainReverseGeocoder geocoder = loadGeocoder();
        ReverseGeocodeResult r = geocoder.reverse(40.4167, -3.70325);
        assertEquals("28079", r.municipality().id(), "Madrid municipality id");
        assertEquals("Madrid", r.municipality().name(), "Madrid municipality name");
        assertEquals("28", r.province().id(), "Madrid province id");
        assertEquals("13", r.autonomousCommunity().id(), "Madrid community id");
    }

    private static void testReverseLookupBarcelona() {
        SpainReverseGeocoder geocoder = loadGeocoder();
        ReverseGeocodeResult r = geocoder.reverse(41.3851, 2.1734);
        assertEquals("08019", r.municipality().id(), "Barcelona municipality id");
        assertEquals("08", r.province().id(), "Barcelona province id");
        assertEquals("09", r.autonomousCommunity().id(), "Barcelona community id");
    }

    private static void testReverseLookupOutside() {
        SpainReverseGeocoder geocoder = loadGeocoder();
        ReverseGeocodeResult r = geocoder.reverse(0.0, 0.0);
        assertNull(r, "Outside point returns null");
    }

    // ─── Polygon Hole Tests ─────────────────────────────────

    private static void testPolygonHoleInside() {
        List<MunicipalityFeature> features = GeoJsonLoader.load(
                Path.of("src/test/resources/polygon_with_hole.geojson"),
                PropertyMapping.defaultMapping(),
                SpainCatalog.loadDefault()
        );
        SpainReverseGeocoder geocoder = new SpainReverseGeocoder(features);
        assertNotNull(geocoder.reverse(40.1, -3.9), "Point in shell resolves");
    }

    private static void testPolygonHoleOutside() {
        List<MunicipalityFeature> features = GeoJsonLoader.load(
                Path.of("src/test/resources/polygon_with_hole.geojson"),
                PropertyMapping.defaultMapping(),
                SpainCatalog.loadDefault()
        );
        SpainReverseGeocoder geocoder = new SpainReverseGeocoder(features);
        assertNull(geocoder.reverse(40.5, -3.5), "Point in hole returns null");
    }

    // ─── Province Filtering Tests ────────────────────────────

    private static void testProvinceFilter() {
        List<MunicipalityFeature> features = GeoJsonLoader.load(
                Path.of("src/test/resources/sample_municipalities.geojson"),
                PropertyMapping.defaultMapping(),
                SpainCatalog.loadDefault(),
                new HashSet<>(Arrays.asList("28"))
        );
        // Only Madrid province municipalities should be loaded (28079, 28065, 28006)
        assertEquals(3, features.size(), "Province filter 28 loads 3 municipalities");
    }

    private static void testProvinceFilterExclusion() {
        List<MunicipalityFeature> features = GeoJsonLoader.load(
                Path.of("src/test/resources/sample_municipalities.geojson"),
                PropertyMapping.defaultMapping(),
                SpainCatalog.loadDefault(),
                new HashSet<>(Arrays.asList("28"))
        );
        SpainReverseGeocoder geocoder = new SpainReverseGeocoder(features);
        // Barcelona should not be found when only Madrid province is loaded
        assertNull(geocoder.reverse(41.3851, 2.1734), "Barcelona excluded by province filter");
    }

    // ─── TextNormalizer Tests ────────────────────────────────

    private static void testNormalizerBasic() {
        assertEquals("madrid", TextNormalizer.normalize("Madrid"), "Normalize basic lowercase");
    }

    private static void testNormalizerAccents() {
        assertEquals("cordoba", TextNormalizer.normalize("Córdoba"), "Normalize accented ó");
        assertEquals("alcala de henares", TextNormalizer.normalize("Alcalá de Henares"), "Normalize accented á");
        assertEquals("malaga", TextNormalizer.normalize("Málaga"), "Normalize accented á");
        assertEquals("caceres", TextNormalizer.normalize("Cáceres"), "Normalize accented á");
        assertEquals("leon", TextNormalizer.normalize("León"), "Normalize accented ó");
        assertEquals("espana", TextNormalizer.normalize("España"), "Normalize ñ");
    }

    private static void testNormalizerNull() {
        assertEquals("", TextNormalizer.normalize(null), "Normalize null");
        assertEquals("", TextNormalizer.normalize(""), "Normalize empty");
    }

    private static void testNormalizerSpecialChars() {
        assertEquals("sant cugat del valles", TextNormalizer.normalize("Sant Cugat del Vallès"), "Normalize è");
        assertEquals("donana", TextNormalizer.normalize("Doñana"), "Normalize ñ");
    }

    // ─── Name Search Exact Tests ─────────────────────────────

    private static void testSearchExactName() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByName("Madrid");
        assertEquals(1, results.size(), "Search exact 'Madrid' finds 1 result");
        assertEquals("28079", results.get(0).municipality().id(), "Search exact 'Madrid' returns correct id");
    }

    private static void testSearchExactNameCaseInsensitive() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByName("MADRID");
        assertEquals(1, results.size(), "Search 'MADRID' (uppercase) finds 1 result");

        List<ReverseGeocodeResult> results2 = index.searchByName("madrid");
        assertEquals(1, results2.size(), "Search 'madrid' (lowercase) finds 1 result");
    }

    private static void testSearchExactNameAccentInsensitive() {
        MunicipalityIndex index = loadIndex();
        // Search without accent should find Córdoba
        List<ReverseGeocodeResult> results = index.searchByName("Cordoba");
        assertEquals(1, results.size(), "Search 'Cordoba' (no accent) finds Córdoba");
        assertEquals("14021", results.get(0).municipality().id(), "Search 'Cordoba' returns correct id");

        // Search with accent should also work
        List<ReverseGeocodeResult> results2 = index.searchByName("Córdoba");
        assertEquals(1, results2.size(), "Search 'Córdoba' (with accent) also finds it");
    }

    private static void testSearchExactNameNotFound() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByName("NoExiste");
        assertEquals(0, results.size(), "Search for non-existent name returns empty");
    }

    // ─── Name Search Partial Tests ───────────────────────────

    private static void testSearchPartialName() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByNameContains("madri");
        assertEquals(1, results.size(), "Partial search 'madri' finds Madrid");

        List<ReverseGeocodeResult> results2 = index.searchByNameContains("barcel");
        assertEquals(1, results2.size(), "Partial search 'barcel' finds Barcelona");
    }

    private static void testSearchPartialNameAccentInsensitive() {
        MunicipalityIndex index = loadIndex();
        // Search partial without accent for Córdoba
        List<ReverseGeocodeResult> results = index.searchByNameContains("cordo");
        assertEquals(1, results.size(), "Partial search 'cordo' finds Córdoba");

        // Search partial for Alcalá
        List<ReverseGeocodeResult> results2 = index.searchByNameContains("alcala");
        assertEquals(1, results2.size(), "Partial search 'alcala' finds Alcalá de Henares");
    }

    // ─── Province + Name Search Tests ────────────────────────

    private static void testSearchByProvinceAndName() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByProvinceAndName("Madrid", "Getafe");
        assertEquals(1, results.size(), "Search Madrid/Getafe finds 1 result");
        assertEquals("28065", results.get(0).municipality().id(), "Search Madrid/Getafe returns correct id");
    }

    private static void testSearchByProvinceIdAndName() {
        MunicipalityIndex index = loadIndex();
        List<ReverseGeocodeResult> results = index.searchByProvinceAndName("28", "Getafe");
        assertEquals(1, results.size(), "Search province 28/Getafe finds 1 result");
    }

    // ─── Geometry JSON Tests ─────────────────────────────────

    private static void testGeometryJsonContainsType() {
        SpainReverseGeocoder geocoder = loadGeocoder();
        ReverseGeocodeResult r = geocoder.reverse(40.4167, -3.70325);
        String json = r.geometry().toJson();
        assertTrue(json.contains("\"type\":\"Polygon\""), "Geometry JSON contains type Polygon");
    }

    private static void testGeometryJsonContainsCoordinates() {
        SpainReverseGeocoder geocoder = loadGeocoder();
        ReverseGeocodeResult r = geocoder.reverse(40.4167, -3.70325);
        String json = r.geometry().toJson();
        assertTrue(json.contains("\"coordinates\":"), "Geometry JSON contains coordinates");
    }

    // ─── SpainCatalog Tests ──────────────────────────────────

    private static void testCatalogProvinceName() {
        SpainCatalog catalog = SpainCatalog.loadDefault();
        assertNotNull(catalog.provinceName("28"), "Province 28 has a name");
    }

    private static void testCatalogCommunityForProvince() {
        SpainCatalog catalog = SpainCatalog.loadDefault();
        assertEquals("13", catalog.communityIdForProvince("28"), "Province 28 belongs to community 13 (Madrid)");
        assertEquals("09", catalog.communityIdForProvince("08"), "Province 08 belongs to community 09 (Cataluña)");
    }

    // ─── JsonEscaper Tests ───────────────────────────────────

    private static void testEscaperBasic() {
        assertEquals("hello", JsonEscaper.escape("hello"), "Escape plain string");
    }

    private static void testEscaperSpecialChars() {
        assertTrue(JsonEscaper.escape("a\"b").contains("\\\""), "Escape double quote");
        assertTrue(JsonEscaper.escape("a\\b").contains("\\\\"), "Escape backslash");
        assertTrue(JsonEscaper.escape("a\nb").contains("\\n"), "Escape newline");
    }

    private static void testEscaperNull() {
        assertEquals("", JsonEscaper.escape(null), "Escape null returns empty");
    }

    // ─── SpainGeo Builder Tests ──────────────────────────────

    private static void testBuilderWithExternalFile() {
        SpainGeo geo = SpainGeo.builder()
                .geoJsonPath(Path.of("src/test/resources/sample_municipalities.geojson"))
                .build();
        ReverseGeocodeResult r = geo.reverse(40.4167, -3.70325);
        assertEquals("28079", r.municipality().id(), "Builder with external file lookup works");
    }

    private static void testBuilderSize() {
        SpainGeo geo = SpainGeo.builder()
                .geoJsonPath(Path.of("src/test/resources/sample_municipalities.geojson"))
                .build();
        assertEquals(6, geo.size(), "Builder loads all 6 municipalities from sample");
    }

    private static void testListCommunities() {
        SpainGeo geo = SpainGeo.builder().build();
        List<AdminDivision> communities = geo.listCommunities();
        assertEquals(19, communities.size(), "List all communities returns 19 results");
    }

    private static void testListProvinces() {
        SpainGeo geo = SpainGeo.builder().build();
        List<AdminDivision> allProvinces = geo.listProvinces(null);
        assertTrue(allProvinces.size() >= 52, "List all provinces returns at least 52 results");

        List<AdminDivision> madridProvinces = geo.listProvinces("13"); // Madrid
        assertEquals(1, madridProvinces.size(), "Madrid community has 1 province");
        assertEquals("Madrid", madridProvinces.get(0).name(), "Province in Madrid community is Madrid");
    }

    private static void testListMunicipalitiesByProvince() {
        SpainGeo geo = SpainGeo.builder()
                .geoJsonPath(Path.of("src/test/resources/sample_municipalities.geojson"))
                .build();
        List<ReverseGeocodeResult> results = geo.listMunicipalitiesByProvince("28");
        assertEquals(3, results.size(), "Madrid province in sample has 3 municipalities");
    }

    private static void testListMunicipalitiesByCommunity() {
        SpainGeo geo = SpainGeo.builder()
                .geoJsonPath(Path.of("src/test/resources/sample_municipalities.geojson"))
                .build();
        List<ReverseGeocodeResult> results = geo.listMunicipalitiesByCommunity("13");
        assertEquals(3, results.size(), "Madrid community in sample has 3 municipalities");
    }

    private static void testResultToJsonWithoutGeometry() {
        SpainGeo geo = SpainGeo.builder()
                .geoJsonPath(Path.of("src/test/resources/sample_municipalities.geojson"))
                .build();
        ReverseGeocodeResult r = geo.reverse(40.4167, -3.70325);
        String jsonWith = r.toJson(true);
        String jsonWithout = r.toJson(false);

        assertTrue(jsonWith.contains("\"geometry\": {") || jsonWith.contains("\"geometry\":{"), "JSON with geometry contains object");
        assertTrue(jsonWithout.contains("\"geometry\": null"), "JSON without geometry has null");
    }
}
