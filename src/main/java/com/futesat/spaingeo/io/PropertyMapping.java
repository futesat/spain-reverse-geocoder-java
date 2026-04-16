package com.futesat.spaingeo.io;

import java.util.Arrays;
import java.util.List;

public final class PropertyMapping {
    private final List<String> municipalityIdCandidates;
    private final List<String> municipalityNameCandidates;
    private final List<String> provinceIdCandidates;
    private final List<String> provinceNameCandidates;
    private final List<String> autonomousCommunityIdCandidates;
    private final List<String> autonomousCommunityNameCandidates;

    public PropertyMapping(
            List<String> municipalityIdCandidates,
            List<String> municipalityNameCandidates,
            List<String> provinceIdCandidates,
            List<String> provinceNameCandidates,
            List<String> autonomousCommunityIdCandidates,
            List<String> autonomousCommunityNameCandidates
    ) {
        this.municipalityIdCandidates = municipalityIdCandidates;
        this.municipalityNameCandidates = municipalityNameCandidates;
        this.provinceIdCandidates = provinceIdCandidates;
        this.provinceNameCandidates = provinceNameCandidates;
        this.autonomousCommunityIdCandidates = autonomousCommunityIdCandidates;
        this.autonomousCommunityNameCandidates = autonomousCommunityNameCandidates;
    }

    public List<String> municipalityIdCandidates() { return municipalityIdCandidates; }
    public List<String> municipalityNameCandidates() { return municipalityNameCandidates; }
    public List<String> provinceIdCandidates() { return provinceIdCandidates; }
    public List<String> provinceNameCandidates() { return provinceNameCandidates; }
    public List<String> autonomousCommunityIdCandidates() { return autonomousCommunityIdCandidates; }
    public List<String> autonomousCommunityNameCandidates() { return autonomousCommunityNameCandidates; }

    public static PropertyMapping defaultMapping() {
        return new PropertyMapping(
                Arrays.asList("municipalityId", "municipality_id", "id", "CODIGO", "CMUNI", "CUMUN", "INE_MUNI", "CODMUN"),
                Arrays.asList("municipalityName", "municipality_name", "name", "LITERAL", "MUNICIPIO", "NOMBRE", "NMUNI"),
                Arrays.asList("provinceId", "province_id", "provinciaId", "CPRO", "CPROV", "INE_PROV", "CODPROV"),
                Arrays.asList("provinceName", "province_name", "provinciaName", "PROVINCIA", "NPRO", "NPROV"),
                Arrays.asList("autonomousCommunityId", "autonomous_community_id", "communityId", "CCAA", "CCA", "CCAA_ID", "CCAA_COD"),
                Arrays.asList("autonomousCommunityName", "autonomous_community_name", "communityName", "COMUNIDAD", "CCAA_NAME", "NCCAA")
        );
    }
}
