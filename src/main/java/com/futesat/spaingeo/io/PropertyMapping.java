package com.futesat.spaingeo.io;

import java.util.List;

public record PropertyMapping(
        List<String> municipalityIdCandidates,
        List<String> municipalityNameCandidates,
        List<String> provinceIdCandidates,
        List<String> provinceNameCandidates,
        List<String> autonomousCommunityIdCandidates,
        List<String> autonomousCommunityNameCandidates
) {
    public static PropertyMapping defaultMapping() {
        return new PropertyMapping(
                List.of("municipalityId", "municipality_id", "id", "CODIGO", "CMUNI", "CUMUN", "INE_MUNI", "CODMUN"),
                List.of("municipalityName", "municipality_name", "name", "LITERAL", "MUNICIPIO", "NOMBRE", "NMUNI"),
                List.of("provinceId", "province_id", "provinciaId", "CPRO", "CPROV", "INE_PROV", "CODPROV"),
                List.of("provinceName", "province_name", "provinciaName", "PROVINCIA", "NPRO", "NPROV"),
                List.of("autonomousCommunityId", "autonomous_community_id", "communityId", "CCAA", "CCA", "CCAA_ID", "CCAA_COD"),
                List.of("autonomousCommunityName", "autonomous_community_name", "communityName", "COMUNIDAD", "CCAA_NAME", "NCCAA")
        );
    }
}
