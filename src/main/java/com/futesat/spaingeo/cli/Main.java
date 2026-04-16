package com.futesat.spaingeo.cli;

import com.futesat.spaingeo.SpainGeo;
import com.futesat.spaingeo.io.PropertyMappingLoader;
import com.futesat.spaingeo.io.SpainCatalog;
import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class Main {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

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
                builder.geoJsonPath(Paths.get(options.get("--geojson")));
            }
            if (options.containsKey("--provinces")) {
                String[] codes = options.get("--provinces").split(",");
                for (String code : codes) {
                    builder.provinces(code.trim());
                }
            }
            if (options.containsKey("--mapping")) {
                builder.mapping(PropertyMappingLoader.load(Paths.get(options.get("--mapping"))));
            }
            if (options.containsKey("--catalog")) {
                builder.catalog(SpainCatalog.load(Paths.get(options.get("--catalog"))));
            }
            if (options.containsKey("--low-precision")) {
                builder.lowPrecision(true);
            }

            SpainGeo spainGeo = builder.build();

            switch (command) {
                case "lookup" -> handleLookup(spainGeo, options);
                case "search" -> handleSearch(spainGeo, options);
                case "list" -> handleList(spainGeo, args, options);
                case "batch" -> handleBatch(spainGeo, options);
                case "demo" -> handleDemo(spainGeo, options);
                default -> throw new IllegalArgumentException("Unsupported command: " + command + 
                        ". Use 'lookup', 'search', 'list', 'batch', or 'demo'.");
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

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required option: --name");
        }

        List<ReverseGeocodeResult> results;

        if (province != null && !province.trim().isEmpty()) {
            results = spainGeo.search(province, name);
        } else if (partial) {
            results = spainGeo.searchByNameContains(name);
        } else {
            results = spainGeo.searchByName(name);
        }

        printResults(results);
    }

    private static void handleBatch(SpainGeo spainGeo, Map<String, String> options) throws IOException {
        String inPath = options.get("--in");
        if (inPath == null) throw new IllegalArgumentException("Missing --in <path>");
        
        Path path = Paths.get(inPath);
        if (!Files.exists(path)) throw new IllegalArgumentException("File not found: " + inPath);

        System.out.println(BOLD + "Processing batch: " + YELLOW + inPath + RESET);
        
        try (Stream<String> lines = Files.lines(path)) {
            List<String> results = lines
                .filter(line -> !line.trim().isEmpty())
                .map(line -> {
                    String[] parts = line.split("[,; \t]");
                    if (parts.length < 2) return null;
                    try {
                        double lat = Double.parseDouble(parts[0].trim());
                        double lon = Double.parseDouble(parts[1].trim());
                        ReverseGeocodeResult res = spainGeo.reverse(lat, lon);
                        return res != null ? res.toJson() : null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
                
            printJsonList(results);
            System.out.println(GREEN + "✓ Processed " + results.size() + " matches." + RESET);
        }
    }

    private static void handleDemo(SpainGeo spainGeo, Map<String, String> options) throws IOException {
        int port = options.containsKey("--port") ? Integer.parseInt(options.get("--port")) : 8080;
        com.futesat.spaingeo.demo.DemoServer server = new com.futesat.spaingeo.demo.DemoServer(spainGeo, port);
        server.start();
        
        System.out.println(BOLD + CYAN + "Press Ctrl+C to stop the server." + RESET);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void handleList(SpainGeo spainGeo, String[] args, Map<String, String> options) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Missing list subcommand (communities, provinces, municipalities)");
        }
        String sub = args[1];
        boolean geometry = options.containsKey("--geometry");

        switch (sub) {
            case "communities" -> printJsonList(spainGeo.listCommunities().stream()
                    .map(com.futesat.spaingeo.model.AdminDivision::toJson).toList());
            case "provinces" -> {
                String communityId = options.get("--community");
                printJsonList(spainGeo.listProvinces(communityId).stream()
                        .map(com.futesat.spaingeo.model.AdminDivision::toJson).toList());
            }
            case "municipalities" -> {
                String pId = options.get("--province");
                String cId = options.get("--community");
                List<ReverseGeocodeResult> res = pId != null ? spainGeo.listMunicipalitiesByProvince(pId) 
                        : spainGeo.listMunicipalitiesByCommunity(cId);
                if (res == null) throw new IllegalArgumentException("Filter required");
                printJsonList(res.stream().map(r -> r.toJson(geometry)).toList());
            }
            default -> throw new IllegalArgumentException("Unknown list: " + sub);
        }
    }

    private static void printResults(List<ReverseGeocodeResult> results) {
        printJsonList(results.stream().map(ReverseGeocodeResult::toJson).toList());
    }

    private static void printJsonList(List<String> items) {
        System.out.println("[");
        for (int i = 0; i < items.size(); i++) {
            System.out.print(items.get(i));
            if (i < items.size() - 1) System.out.println(",");
            else System.out.println();
        }
        System.out.println("]");
    }

    private static Map<String, String> parseOptions(String[] args, int startIndex) {
        Map<String, String> options = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) throw new IllegalArgumentException("Invalid option: " + arg);
            if (arg.equals("--partial") || arg.equals("--low-precision") || arg.equals("--geometry")) {
                options.put(arg, "true");
                continue;
            }
            if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + arg);
            options.put(arg, args[++i]);
        }
        return options;
    }

    private static double requiredDouble(Map<String, String> options, String key) {
        String val = options.get(key);
        if (val == null) throw new IllegalArgumentException("Missing " + key);
        return Double.parseDouble(val);
    }

    private static void printHelp() {
        System.out.println(CYAN + BOLD + "Spain Reverse Geocoder (Java 21 Edition)" + RESET + "\n" +
                "\n" +
                BOLD + "Commands:" + RESET + "\n" +
                "  lookup --lat <lat> --lon <lon>   Reverse lookup\n" +
                "  search --name <query> [--partial] Search municipality\n" +
                "  batch  --in <file>              Process CSV\n" +
                "  demo   [--port 8080]             Interactive map\n" +
                "  list   communities/provinces/municipalities\n");
    }
}
