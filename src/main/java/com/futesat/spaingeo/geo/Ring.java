package com.futesat.spaingeo.geo;

import java.util.List;

public final class Ring {
    private static final double EPS = 1e-12;

    private final List<Coordinate> coordinates;
    private final Envelope envelope;

    public Ring(List<Coordinate> coordinates) {
        if (coordinates == null || coordinates.size() < 4) {
            throw new IllegalArgumentException("A ring needs at least 4 coordinates.");
        }
        this.coordinates = List.copyOf(coordinates);
        this.envelope = computeEnvelope(coordinates);
    }

    public Envelope envelope() {
        return envelope;
    }

    public boolean covers(double x, double y) {
        if (!envelope.contains(x, y)) {
            return false;
        }

        boolean inside = false;
        int n = coordinates.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Coordinate a = coordinates.get(j);
            Coordinate b = coordinates.get(i);

            if (pointOnSegment(x, y, a, b)) {
                return true;
            }

            boolean intersects = ((a.y() > y) != (b.y() > y))
                    && (x < (b.x() - a.x()) * (y - a.y()) / (b.y() - a.y()) + a.x());
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate c = coordinates.get(i);
            if (i > 0) sb.append(",");
            sb.append("[").append(c.x()).append(",").append(c.y()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean pointOnSegment(double px, double py, Coordinate a, Coordinate b) {
        double cross = (px - a.x()) * (b.y() - a.y()) - (py - a.y()) * (b.x() - a.x());
        if (Math.abs(cross) > EPS) {
            return false;
        }
        double dot = (px - a.x()) * (px - b.x()) + (py - a.y()) * (py - b.y());
        return dot <= EPS;
    }

    private static Envelope computeEnvelope(List<Coordinate> coordinates) {
        Coordinate first = coordinates.get(0);
        Envelope env = Envelope.fromPoint(first.x(), first.y());
        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate c = coordinates.get(i);
            env = env.expandToInclude(c.x(), c.y());
        }
        return env;
    }
}
