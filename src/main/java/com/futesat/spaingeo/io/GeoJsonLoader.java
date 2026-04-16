package com.futesat.spaingeo.io;

import com.futesat.spaingeo.core.MunicipalityFeature;
import com.futesat.spaingeo.geo.Coordinate;
import com.futesat.spaingeo.geo.Geometry;
import com.futesat.spaingeo.geo.MultiPolygonGeometry;
import com.futesat.spaingeo.geo.PolygonGeometry;
import com.futesat.spaingeo.geo.Ring;
import com.futesat.spaingeo.model.AdminDivision;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class GeoJsonLoader {
    private GeoJsonLoader() {
    }

    public static List<MunicipalityFeature> load(Path geoJsonPath, PropertyMapping mapping, SpainCatalog catalog) {
        return load(geoJsonPath, mapping, catalog, null);
    }

    public static List<MunicipalityFeature> load(Path geoJsonPath, PropertyMapping mapping, SpainCatalog catalog, java.util.Set<String> provinceFilter) {
        try {
            String json = Files.readString(geoJsonPath, StandardCharsets.UTF_8);
            return loadFromJson(json, mapping, catalog, provinceFilter);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read GeoJSON: " + geoJsonPath, e);
        }
    }

    public static List<MunicipalityFeature> load(java.io.InputStream inputStream, PropertyMapping mapping, SpainCatalog catalog, java.util.Set<String> provinceFilter) {
        try {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return loadFromJson(json, mapping, catalog, provinceFilter);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read GeoJSON from stream", e);
        }
    }

    private static List<MunicipalityFeature> loadFromJson(String json, PropertyMapping mapping, SpainCatalog catalog, java.util.Set<String> provinceFilter) {
        Map<String, Object> root = (Map<String, Object>) MiniJsonParser.parse(json);
        String type = root.get("type").toString();
        if (!"FeatureCollection".equals(type)) {
            throw new IllegalArgumentException("GeoJSON root must be a FeatureCollection.");
        }

        List<Object> features = (List<Object>) root.get("features");
        List<MunicipalityFeature> results = new ArrayList<>(features.size());
        for (Object featureObject : features) {
            Map<String, Object> feature = (Map<String, Object>) featureObject;
            Map<String, Object> properties = (Map<String, Object>) feature.getOrDefault("properties", Map.of());
            Map<String, Object> geometryObject = (Map<String, Object>) feature.get("geometry");
            if (geometryObject == null) {
                continue;
            }

            // Memory optimization: early exit if province filter is set
            if (provinceFilter != null && !provinceFilter.isEmpty()) {
                String mId = normalizeMunicipalityId(readFirst(properties, mapping.municipalityIdCandidates()));
                if (mId != null && mId.length() >= 2) {
                    String pId = mId.substring(0, 2);
                    if (!provinceFilter.contains(pId)) {
                        continue;
                    }
                }
            }

            Geometry geometry = parseGeometry(geometryObject);
            ReverseGeocodeResult result = parseResult(properties, mapping, catalog, geometry);
            results.add(new MunicipalityFeature(result, geometry, geometry.envelope()));
        }
        return results;
    }

    private static ReverseGeocodeResult parseResult(Map<String, Object> properties, PropertyMapping mapping, SpainCatalog catalog, Geometry geometry) {
        String municipalityId = normalizeMunicipalityId(readFirst(properties, mapping.municipalityIdCandidates()));
        String municipalityName = readFirst(properties, mapping.municipalityNameCandidates());
        if ((municipalityName == null || municipalityName.isBlank()) && municipalityId != null) {
            municipalityName = catalog.municipalityName(municipalityId);
        }

        if (municipalityId == null || municipalityId.isBlank()) {
            throw new IllegalArgumentException("Feature is missing a municipality id.");
        }
        if (municipalityName == null || municipalityName.isBlank()) {
            throw new IllegalArgumentException("Feature is missing a municipality name.");
        }

        String provinceId = normalizeProvinceId(readFirst(properties, mapping.provinceIdCandidates()));
        if (provinceId == null && municipalityId.length() >= 2) {
            provinceId = municipalityId.substring(0, 2);
        }

        String provinceName = readFirst(properties, mapping.provinceNameCandidates());
        if ((provinceName == null || provinceName.isBlank()) && provinceId != null) {
            provinceName = catalog.provinceName(provinceId);
        }

        String communityId = normalizeCommunityId(readFirst(properties, mapping.autonomousCommunityIdCandidates()));
        if (communityId == null && provinceId != null) {
            communityId = catalog.communityIdForProvince(provinceId);
        }

        String communityName = readFirst(properties, mapping.autonomousCommunityNameCandidates());
        if ((communityName == null || communityName.isBlank()) && communityId != null) {
            communityName = catalog.communityName(communityId);
        }

        if (provinceId == null || provinceName == null || communityId == null || communityName == null) {
            throw new IllegalArgumentException(
                    "Could not resolve province/community for municipality " + municipalityId + ". " +
                            "Provide those properties explicitly or use 5-digit municipality codes."
            );
        }

        return new ReverseGeocodeResult(
                new AdminDivision(municipalityId, municipalityName),
                new AdminDivision(provinceId, provinceName),
                new AdminDivision(communityId, communityName),
                geometry
        );
    }

    private static String readFirst(Map<String, Object> properties, List<String> candidates) {
        for (String key : candidates) {
            Object value = properties.get(key);
            if (value != null) {
                if (value instanceof Number number) {
                    long longValue = number.longValue();
                    return Long.toString(longValue);
                }
                return value.toString();
            }
        }
        return null;
    }

    private static String normalizeMunicipalityId(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 5) return digits;
        if (digits.length() > 5) return digits.substring(0, 5);
        if (!digits.isEmpty()) return String.format("%05d", Integer.parseInt(digits));
        return raw;
    }

    private static String normalizeProvinceId(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() >= 2) return digits.substring(0, 2);
        if (digits.length() == 1) return "0" + digits;
        return raw;
    }

    private static String normalizeCommunityId(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() >= 2) return digits.substring(0, 2);
        if (digits.length() == 1) return "0" + digits;
        return raw;
    }

    private static Geometry parseGeometry(Map<String, Object> geometry) {
        String type = geometry.get("type").toString();
        Object coordinates = geometry.get("coordinates");
        return switch (type) {
            case "Polygon" -> parsePolygon((List<Object>) coordinates);
            case "MultiPolygon" -> parseMultiPolygon((List<Object>) coordinates);
            default -> throw new IllegalArgumentException("Unsupported geometry type: " + type);
        };
    }

    private static PolygonGeometry parsePolygon(List<Object> polygonCoordinates) {
        if (polygonCoordinates.isEmpty()) {
            throw new IllegalArgumentException("Empty polygon.");
        }
        Ring shell = parseRing((List<Object>) polygonCoordinates.get(0));
        List<Ring> holes = new ArrayList<>();
        for (int i = 1; i < polygonCoordinates.size(); i++) {
            holes.add(parseRing((List<Object>) polygonCoordinates.get(i)));
        }
        return new PolygonGeometry(shell, holes);
    }

    private static MultiPolygonGeometry parseMultiPolygon(List<Object> multiPolygonCoordinates) {
        List<PolygonGeometry> polygons = new ArrayList<>();
        for (Object polygonObject : multiPolygonCoordinates) {
            polygons.add(parsePolygon((List<Object>) polygonObject));
        }
        return new MultiPolygonGeometry(polygons);
    }

    private static Ring parseRing(List<Object> ringCoordinates) {
        List<Coordinate> points = new ArrayList<>(ringCoordinates.size());
        for (Object pointObject : ringCoordinates) {
            List<Object> point = (List<Object>) pointObject;
            if (point.size() < 2) {
                throw new IllegalArgumentException("Invalid coordinate in GeoJSON ring.");
            }
            double x = asDouble(point.get(0));
            double y = asDouble(point.get(1));
            points.add(new Coordinate(x, y));
        }
        return new Ring(points);
    }

    private static double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
