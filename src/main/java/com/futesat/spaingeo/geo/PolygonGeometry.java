package com.futesat.spaingeo.geo;

import java.util.List;

public final class PolygonGeometry implements Geometry {
    private final Ring shell;
    private final List<Ring> holes;
    private final Envelope envelope;

    public PolygonGeometry(Ring shell, List<Ring> holes) {
        this.shell = shell;
        this.holes = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(holes));
        this.envelope = shell.envelope();
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
        if (!shell.covers(x, y)) {
            return false;
        }
        for (Ring hole : holes) {
            if (hole.covers(x, y)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Polygon\",\"coordinates\":[");
        sb.append(shell.toJson());
        for (Ring hole : holes) {
            sb.append(",");
            sb.append(hole.toJson());
        }
        sb.append("]}");
        return sb.toString();
    }
}
