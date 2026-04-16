package com.futesat.spaingeo.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class PropertyMappingLoader {
    private PropertyMappingLoader() {
    }

    public static PropertyMapping load(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String json = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Object> root = (Map<String, Object>) MiniJsonParser.parse(json);
            return new PropertyMapping(
                    listOrSingle(root, "municipalityId", PropertyMapping.defaultMapping().municipalityIdCandidates()),
                    listOrSingle(root, "municipalityName", PropertyMapping.defaultMapping().municipalityNameCandidates()),
                    listOrSingle(root, "provinceId", PropertyMapping.defaultMapping().provinceIdCandidates()),
                    listOrSingle(root, "provinceName", PropertyMapping.defaultMapping().provinceNameCandidates()),
                    listOrSingle(root, "autonomousCommunityId", PropertyMapping.defaultMapping().autonomousCommunityIdCandidates()),
                    listOrSingle(root, "autonomousCommunityName", PropertyMapping.defaultMapping().autonomousCommunityNameCandidates())
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read mapping file: " + path, e);
        }
    }

    private static List<String> listOrSingle(Map<String, Object> root, String key, List<String> defaultValue) {
        Object value = root.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            String s = (String) value;
            return java.util.Arrays.asList(s);
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return list.stream().map(Object::toString).collect(java.util.stream.Collectors.toList());
        }
        return defaultValue;
    }
}
