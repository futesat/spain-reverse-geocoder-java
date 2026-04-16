package com.futesat.spaingeo.cli;

import com.futesat.spaingeo.SpainGeo;
import com.futesat.spaingeo.io.PropertyMappingLoader;
import com.futesat.spaingeo.io.SpainCatalog;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "help".equals(args[0])) {
                printHelp();
                return;
            }
            String command = args[0];
            int optionsStartIndex = command.equals("list") ? 2 : 1;
            Map<String, String> options = parseOptions(args, optionsStartIndex);
            SpainGeo.Builder builder = SpainGeo.builder();

            if (options.containsKey("--geojson")) {
                builder.geoJsonPath(Path.of(options.get("--geojson")));
            }
            if (options.containsKey("--provinces")) {
                String[] codes = options.get("--provinces").split(",");
                for (String code : codes) {
                    builder.provinces(code.trim());
                }
            }
            if (options.containsKey("--mapping")) {
                builder.mapping(PropertyMappingLoader.load(Path.of(options.get("--mapping"))));
            }
            if (options.containsKey("--catalog")) {
                builder.catalog(SpainCatalog.load(Path.of(options.get("--catalog"))));
            }
            if (options.containsKey("--low-precision")) {
                builder.lowPrecision(true);
            }

            SpainGeo spainGeo = builder.build();

            switch (command) {
                case "lookup" -> handleLookup(spainGeo, options);
                case "search" -> handleSearch(spainGeo, options);
                case "list" -> handleList(spainGeo, args, options);
                default -> throw new IllegalArgumentException("Unsupported command: " + command + ". Use 'lookup', 'search', or 'list'.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleLookup(SpainGeo spainGeo, Map<String, String> options) {
        double lat = requiredDouble(options, "--lat");
        double lon = requiredDouble(options, "--lon");

        ReverseGeocodeResult result = spainGeo.reverse(lat, lon);

        if (result == null) {
            System.out.println("{\n  \"result\": null\n}");
            System.exit(2);
            return;
        }
        System.out.println(result.toJson());
    }

    private static void handleSearch(SpainGeo spainGeo, Map<String, String> options) {
        String name = options.get("--name");
        String province = options.get("--province");
        boolean partial = options.containsKey("--partial");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing required option: --name");
        }

        List<ReverseGeocodeResult> results;

        if (province != null && !province.isBlank()) {
            results = spainGeo.search(province, name);
        } else if (partial) {
            results = spainGeo.searchByNameContains(name);
        } else {
            results = spainGeo.searchByName(name);
        }

        System.out.println("[");
        for (int i = 0; i < results.size(); i++) {
            System.out.print(results.get(i).toJson());
            if (i < results.size() - 1) {
                System.out.print(",");
            }
        }
        System.out.println("]");
    }

    private static void handleList(SpainGeo spainGeo, String[] args, Map<String, String> options) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Missing subcommand for 'list'. Use 'communities', 'provinces', or 'municipalities'.");
        }
        String sub = args[1];
        boolean geometry = options.containsKey("--geometry");

        switch (sub) {
            case "communities" -> {
                printList(spainGeo.listCommunities().stream().map(com.futesat.spaingeo.model.AdminDivision::toJson).toList());
            }
            case "provinces" -> {
                String communityId = options.get("--community");
                printList(spainGeo.listProvinces(communityId).stream().map(com.futesat.spaingeo.model.AdminDivision::toJson).toList());
            }
            case "municipalities" -> {
                String provinceId = options.get("--province");
                String communityId = options.get("--community");
                List<ReverseGeocodeResult> results;
                if (provinceId != null) {
                    results = spainGeo.listMunicipalitiesByProvince(provinceId);
                } else if (communityId != null) {
                    results = spainGeo.listMunicipalitiesByCommunity(communityId);
                } else {
                    throw new IllegalArgumentException("Missing --province or --community filter for 'list municipalities'.");
                }
                printList(results.stream().map(r -> r.toJson(geometry)).toList());
            }
            default -> throw new IllegalArgumentException("Unsupported list subcommand: " + sub);
        }
    }

    private static void printList(List<String> items) {
        System.out.println("[");
        for (int i = 0; i < items.size(); i++) {
            System.out.print(items.get(i));
            if (i < items.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");
    }

    private static Map<String, String> parseOptions(String[] args, int startIndex) {
        Map<String, String> options = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Expected option starting with --, got: " + arg);
            }
            // Boolean flags (no value)
            if ("--partial".equals(arg) || "--low-precision".equals(arg) || "--geometry".equals(arg)) {
                options.put(arg, "true");
                continue;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for option: " + arg);
            }
            options.put(arg, args[++i]);
        }
        return options;
    }

    private static double requiredDouble(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option: " + key);
        }
        return Double.parseDouble(value);
    }

    private static void printHelp() {
        System.out.println("""
                Spain Reverse Geocoder (offline, exact polygons)

                Commands:
                  lookup    Reverse geocode a coordinate to a municipality
                  search    Search municipalities by name

                Lookup usage:
                  java -jar spain-reverse-geocoder.jar lookup --lat <lat> --lon <lon>
                
                List usage:
                  java -jar spain-reverse-geocoder.jar list communities
                  java -jar spain-reverse-geocoder.jar list provinces [--community <id>]
                  java -jar spain-reverse-geocoder.jar list municipalities --province <id> [--geometry]
                  java -jar spain-reverse-geocoder.jar list municipalities --community <id> [--geometry]

                Search usage:
                  java -jar spain-reverse-geocoder.jar search --name <query>
                  java -jar spain-reverse-geocoder.jar search --name <query> --partial
                  java -jar spain-reverse-geocoder.jar search --province <province> --name <query>

                Common options:
                  --geojson <path>     Path to GeoJSON file (optional, uses embedded default)
                  --provinces <ids>    Comma-separated province codes to filter (e.g. 28,08)
                  --mapping <path>     Path to property mapping JSON
                  --catalog <path>     Path to administrative catalog JSON
                  --low-precision      Use low-precision GeoJSON (~75MB vs ~90MB)
                """);
    }
}
