package com.futesat.spaingeo.core;

import com.futesat.spaingeo.geo.Envelope;
import com.futesat.spaingeo.geo.Geometry;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

public record MunicipalityFeature(
        ReverseGeocodeResult result,
        Geometry geometry,
        Envelope envelope
) {
}
