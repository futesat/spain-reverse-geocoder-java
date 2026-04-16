package com.futesat.spaingeo;

import com.futesat.spaingeo.core.MunicipalityFeature;
import com.futesat.spaingeo.core.MunicipalityIndex;
import com.futesat.spaingeo.core.SpainReverseGeocoder;
import com.futesat.spaingeo.io.GeoJsonLoader;
import com.futesat.spaingeo.io.PropertyMapping;
import com.futesat.spaingeo.io.SpainCatalog;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main entry point for the Spain Reverse Geocoder library.
 * Use the builder to configure and instantiate.
 *
 * <pre>{@code
 * SpainGeo geo = SpainGeo.builder()
 *     .provinces("28", "08")
 *     .build();
 *
 * // Reverse geocoding
 * ReverseGeocodeResult result = geo.reverse(40.4167, -3.70325);
 *
 * // Name search
 * List<ReverseGeocodeResult> results = geo.searchByName("Madrid");
 * }</pre>
 */
public final class SpainGeo {
    private final SpainReverseGeocoder geocoder;
    private final MunicipalityIndex index;
    private final SpainCatalog catalog;

    private SpainGeo(SpainReverseGeocoder geocoder, MunicipalityIndex index, SpainCatalog catalog) {
        this.geocoder = geocoder;
        this.index = index;
        this.catalog = catalog;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Performs a reverse geocoding lookup.
     *
     * @param lat Latitude (WGS84)
     * @param lon Longitude (WGS84)
     * @return Result with municipality, province, community and geometry, or null if not found
     */
    public ReverseGeocodeResult reverse(double lat, double lon) {
        return geocoder.reverse(lat, lon);
    }

    /**
     * Search municipalities by exact name (accent/case insensitive).
     *
     * @param name municipality name (e.g. "Madrid", "córdoba", "GETAFE")
     * @return list of matching results
     */
    public List<ReverseGeocodeResult> searchByName(String name) {
        return index.searchByName(name);
    }

    /**
     * Search municipalities by partial name (accent/case insensitive, substring match).
     *
     * @param query partial name (e.g. "madri", "barcel")
     * @return list of matching results
     */
    public List<ReverseGeocodeResult> searchByNameContains(String query) {
        return index.searchByNameContains(query);
    }

    /**
     * Search municipalities by province and name.
     * Province can be an INE code or name (accent/case insensitive).
     *
     * @param province province name or 2-digit INE code
     * @param municipality municipality name or partial name
     * @return list of matching results
     */
    public List<ReverseGeocodeResult> search(String province, String municipality) {
        return index.searchByProvinceAndName(province, municipality);
    }

    /**
     * List all autonomous communities.
     */
    public List<com.futesat.spaingeo.model.AdminDivision> listCommunities() {
        return catalog.listCommunities();
    }

    /**
     * List provinces, optionally filtered by community ID.
     */
    public List<com.futesat.spaingeo.model.AdminDivision> listProvinces(String communityId) {
        return catalog.listProvinces(communityId);
    }

    /**
     * List all municipalities in a specific province.
     */
    public List<ReverseGeocodeResult> listMunicipalitiesByProvince(String provinceId) {
        return index.listByProvince(provinceId);
    }

    /**
     * List all municipalities in a specific autonomous community.
     */
    public List<ReverseGeocodeResult> listMunicipalitiesByCommunity(String communityId) {
        return index.listByCommunity(communityId);
    }

    /**
     * Returns the total number of loaded municipalities.
     */
    public int size() {
        return index.size();
    }

    public static final class Builder {
        private Path geoJsonPath;
        private Set<String> provinces = new HashSet<>();
        private PropertyMapping mapping = PropertyMapping.defaultMapping();
        private SpainCatalog catalog;
        private boolean lowPrecision = false;

        public Builder geoJsonPath(Path path) {
            this.geoJsonPath = path;
            return this;
        }

        public Builder provinces(String... provinceCodes) {
            Collections.addAll(this.provinces, provinceCodes);
            return this;
        }

        public Builder provinces(Set<String> provinceCodes) {
            this.provinces = new HashSet<>(provinceCodes);
            return this;
        }

        public Builder mapping(PropertyMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        public Builder catalog(SpainCatalog catalog) {
            this.catalog = catalog;
            return this;
        }

        public Builder lowPrecision(boolean lowPrecision) {
            this.lowPrecision = lowPrecision;
            return this;
        }

        public SpainGeo build() {
            if (catalog == null) {
                catalog = SpainCatalog.loadDefault();
            }

            List<MunicipalityFeature> features;
            if (geoJsonPath != null) {
                features = GeoJsonLoader.load(geoJsonPath, mapping, catalog, provinces);
            } else {
                String resourceName = lowPrecision ? "spain_municipalities_low_precision.geojson" : "spain_municipalities.geojson";
                try (InputStream in = SpainGeo.class.getClassLoader().getResourceAsStream(resourceName)) {
                    if (in == null) {
                        throw new IllegalStateException("Embedded GeoJSON resource '" + resourceName + "' not found in classpath.");
                    }
                    features = GeoJsonLoader.load(in, mapping, catalog, provinces);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load embedded GeoJSON: " + resourceName, e);
                }
            }

            SpainReverseGeocoder geocoder = new SpainReverseGeocoder(features);
            MunicipalityIndex index = new MunicipalityIndex(features);
            return new SpainGeo(geocoder, index, catalog);
        }
    }
}
