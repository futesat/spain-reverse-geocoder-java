package com.futesat.spaingeo.core;

import com.futesat.spaingeo.geo.Envelope;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.util.List;
import java.util.Objects;

public final class SpainReverseGeocoder {
    private final QuadtreeSpatialIndex index;

    public SpainReverseGeocoder(List<MunicipalityFeature> features) {
        Objects.requireNonNull(features, "features");
        if (features.isEmpty()) {
            throw new IllegalArgumentException("At least one municipality feature is required.");
        }
        Envelope bounds = features.get(0).envelope();
        for (int i = 1; i < features.size(); i++) {
            bounds = bounds.expandToInclude(features.get(i).envelope());
        }
        this.index = new QuadtreeSpatialIndex(bounds, 24, 12);
        for (MunicipalityFeature feature : features) {
            this.index.insert(feature);
        }
    }

    public ReverseGeocodeResult reverse(double lat, double lon) {
        List<MunicipalityFeature> candidates = index.query(lon, lat);
        for (MunicipalityFeature candidate : candidates) {
            if (candidate.geometry().covers(lon, lat)) {
                return candidate.result();
            }
        }
        return null;
    }
}
