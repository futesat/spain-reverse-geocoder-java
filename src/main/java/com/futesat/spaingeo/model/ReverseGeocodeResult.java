package com.futesat.spaingeo.model;

public record ReverseGeocodeResult(
        AdminDivision municipality,
        AdminDivision province,
        AdminDivision autonomousCommunity,
        com.futesat.spaingeo.geo.Geometry geometry
) {
    public String toJson() {
        return """
                {
                  "municipality": { "id": "%s", "name": "%s" },
                  "province": { "id": "%s", "name": "%s" },
                  "autonomousCommunity": { "id": "%s", "name": "%s" },
                  "geometry": %s
                }
                """.formatted(
                JsonEscaper.escape(municipality.id()), JsonEscaper.escape(municipality.name()),
                JsonEscaper.escape(province.id()), JsonEscaper.escape(province.name()),
                JsonEscaper.escape(autonomousCommunity.id()), JsonEscaper.escape(autonomousCommunity.name()),
                geometry.toJson()
        );
    }
}
