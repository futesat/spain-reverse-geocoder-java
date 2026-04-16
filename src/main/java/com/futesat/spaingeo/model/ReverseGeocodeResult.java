package com.futesat.spaingeo.model;

public record ReverseGeocodeResult(
        AdminDivision municipality,
        AdminDivision province,
        AdminDivision autonomousCommunity,
        com.futesat.spaingeo.geo.Geometry geometry
) {
    public String toJson() {
        return toJson(true);
    }

    public String toJson(boolean includeGeometry) {
        String geometryJson = (includeGeometry && geometry != null) ? geometry.toJson() : "null";
        return """
                {
                  "municipality": %s,
                  "province": %s,
                  "autonomousCommunity": %s,
                  "geometry": %s
                }
                """.formatted(
                municipality.toJson(),
                province.toJson(),
                autonomousCommunity.toJson(),
                geometryJson
        );
    }
}
