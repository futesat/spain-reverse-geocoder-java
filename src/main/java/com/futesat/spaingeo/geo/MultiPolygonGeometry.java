package com.futesat.spaingeo.geo;

import java.util.List;

public final class MultiPolygonGeometry implements Geometry {
    private final List<PolygonGeometry> polygons;
    private final Envelope envelope;

    public MultiPolygonGeometry(List<PolygonGeometry> polygons) {
        if (polygons == null || polygons.isEmpty()) {
            throw new IllegalArgumentException("A multipolygon needs at least one polygon.");
        }
        this.polygons = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(polygons));
        Envelope env = polygons.get(0).envelope();
        for (int i = 1; i < polygons.size(); i++) {
            env = env.expandToInclude(polygons.get(i).envelope());
        }
        this.envelope = env;
    }

    @Override
    public Envelope envelope() {
        return envelope;
    }

    @Override
    public boolean covers(double x, double y) {
        if (!envelope.contains(x, y)) {
            return false;
        }
        for (PolygonGeometry polygon : polygons) {
            if (polygon.covers(x, y)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MultiPolygon\",\"coordinates\":[");
        for (int i = 0; i < polygons.size(); i++) {
            if (i > 0) sb.append(",");
            // MultiPolygon coordinates are nested one level deeper: list of polygons, each is list of rings
            // We need to extract the coordinates from the polygon's toJson which is {"type":"Polygon", "coordinates": [...]}
            String polyJson = polygons.get(i).toJson();
            int start = polyJson.indexOf("\"coordinates\":") + "\"coordinates\":".length();
            int end = polyJson.lastIndexOf("]}");
            sb.append(polyJson, start, end + 1);
        }
        sb.append("]}");
        return sb.toString();
    }
}
