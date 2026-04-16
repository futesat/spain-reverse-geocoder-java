package com.futesat.spaingeo.model;

public final class ReverseGeocodeResult {
    private final AdminDivision municipality;
    private final AdminDivision province;
    private final AdminDivision autonomousCommunity;
    private final com.futesat.spaingeo.geo.Geometry geometry;

    public ReverseGeocodeResult(AdminDivision municipality, AdminDivision province,
                                AdminDivision autonomousCommunity, com.futesat.spaingeo.geo.Geometry geometry) {
        this.municipality = municipality;
        this.province = province;
        this.autonomousCommunity = autonomousCommunity;
        this.geometry = geometry;
    }

    public AdminDivision municipality() { return municipality; }
    public AdminDivision province() { return province; }
    public AdminDivision autonomousCommunity() { return autonomousCommunity; }
    public com.futesat.spaingeo.geo.Geometry geometry() { return geometry; }
    public String toJson() {
        return toJson(true);
    }

    public String toJson(boolean includeGeometry) {
        String geometryJson = (includeGeometry && geometry != null) ? geometry.toJson() : "null";
        return String.format("{\n  \"municipality\": %s,\n  \"province\": %s,\n  \"autonomousCommunity\": %s,\n  \"geometry\": %s\n}",
                municipality.toJson(),
                province.toJson(),
                autonomousCommunity.toJson(),
                geometryJson
        );
    }
}
