package com.futesat.spaingeo.core;

import com.futesat.spaingeo.geo.Envelope;
import com.futesat.spaingeo.geo.Geometry;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

public final class MunicipalityFeature {
    private final ReverseGeocodeResult result;
    private final Geometry geometry;
    private final Envelope envelope;

    public MunicipalityFeature(ReverseGeocodeResult result, Geometry geometry, Envelope envelope) {
        this.result = result;
        this.geometry = geometry;
        this.envelope = envelope;
    }

    public ReverseGeocodeResult result() { return result; }
    public Geometry geometry() { return geometry; }
    public Envelope envelope() { return envelope; }
}
