package com.futesat.spaingeo.geo;

public interface Geometry {
    Envelope envelope();

    boolean covers(double x, double y);

    String toJson();
}
