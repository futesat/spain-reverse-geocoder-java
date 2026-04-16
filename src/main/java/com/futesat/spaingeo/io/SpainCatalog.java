package com.futesat.spaingeo.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class SpainCatalog {
    private final Map<String, String> provinceNames;
    private final Map<String, String> communityNames;
    private final Map<String, String> municipalityNames;
    private final Map<String, String> provinceToCommunity;

    private SpainCatalog(Map<String, String> provinceNames, Map<String, String> communityNames, Map<String, String> municipalityNames,
            Map<String, String> provinceToCommunity) {
        this.provinceNames = provinceNames;
        this.communityNames = communityNames;
        this.municipalityNames = municipalityNames;
        this.provinceToCommunity = provinceToCommunity;
    }

    public static SpainCatalog loadDefault() {
        try (InputStream in = SpainCatalog.class.getClassLoader()
                .getResourceAsStream("spain_administrative_divisions.json")) {
            if (in == null) {
                throw new IllegalStateException("Resource spain_administrative_divisions.json was not found.");
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load built-in Spain catalog.", e);
        }
    }

    public static SpainCatalog load(Path path) {
        try {
            return fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read catalog: " + path, e);
        }
    }

    public String provinceName(String provinceId) {
        return provinceNames.get(provinceId);
    }

    public String communityName(String communityId) {
        return communityNames.get(communityId);
    }

    public String communityIdForProvince(String provinceId) {
        return provinceToCommunity.get(provinceId);
    }

    public String municipalityName(String municipalityId) {
        return municipalityNames.get(municipalityId);
    }

    private static SpainCatalog fromJson(String json) {
        Object parsed = MiniJsonParser.parse(json);
        Map<String, Object> root = (Map<String, Object>) parsed;
        Map<String, String> provinceNames = new HashMap<>();
        Map<String, String> communityNames = new HashMap<>();
        Map<String, String> municipalityNames = new HashMap<>();

        List<Object> provinces = (List<Object>) root.get("provinces");
        for (Object item : provinces) {
            Map<String, Object> map = (Map<String, Object>) item;
            provinceNames.put(asString(map.get("id")), asString(map.get("name")));
        }

        List<Object> communities = (List<Object>) root.get("autonomousCommunities");
        for (Object item : communities) {
            Map<String, Object> map = (Map<String, Object>) item;
            communityNames.put(asString(map.get("id")), asString(map.get("name")));
        }

        List<Object> municipalityList = (List<Object>) root.get("municipalities");
        if (municipalityList != null) {
            for (Object item : municipalityList) {
                Map<String, Object> map = (Map<String, Object>) item;
                municipalityNames.put(asString(map.get("id")), asString(map.get("name")));
            }
        }

        return new SpainCatalog(provinceNames, communityNames, municipalityNames, buildProvinceToCommunityMap());
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long longValue = number.longValue();
            return String.format("%02d", longValue);
        }
        return value.toString();
    }

    private static Map<String, String> buildProvinceToCommunityMap() {
        Map<String, String> map = new HashMap<>();
        // Andalucía
        for (String province : List.of("04", "11", "14", "18", "21", "23", "29", "41"))
            map.put(province, "01");
        // Aragón
        for (String province : List.of("22", "44", "50"))
            map.put(province, "02");
        // Asturias
        map.put("33", "03");
        // Balears
        map.put("07", "04");
        // Canarias
        for (String province : List.of("35", "38"))
            map.put(province, "05");
        // Cantabria
        map.put("39", "06");
        // Castilla y León
        for (String province : List.of("05", "09", "24", "34", "37", "40", "42", "47", "49"))
            map.put(province, "07");
        // Castilla - La Mancha
        for (String province : List.of("02", "13", "16", "19", "45"))
            map.put(province, "08");
        // Cataluña
        for (String province : List.of("08", "17", "25", "43"))
            map.put(province, "09");
        // Comunitat Valenciana
        for (String province : List.of("03", "12", "46"))
            map.put(province, "10");
        // Extremadura
        for (String province : List.of("06", "10"))
            map.put(province, "11");
        // Galicia
        for (String province : List.of("15", "27", "32", "36"))
            map.put(province, "12");
        // Madrid
        map.put("28", "13");
        // Murcia
        map.put("30", "14");
        // Navarra
        map.put("31", "15");
        // País Vasco
        for (String province : List.of("01", "20", "48"))
            map.put(province, "16");
        // Rioja
        map.put("26", "17");
        // Ceuta
        map.put("51", "18");
        // Melilla
        map.put("52", "19");
        return map;
    }
}
