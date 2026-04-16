package com.futesat.spaingeo.demo;

import com.futesat.spaingeo.SpainGeo;
import com.futesat.spaingeo.model.ReverseGeocodeResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A simple, zero-dependency web server using Virtual Threads (Java 21).
 */
public final class DemoServer {
    private final SpainGeo spainGeo;
    private final int port;

    public DemoServer(SpainGeo spainGeo, int port) {
        this.spainGeo = spainGeo;
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/reverse", new ApiHandler());
        server.createContext("/api/search", new SearchHandler());
        
        // Using Java 21 Virtual Threads for high scalability
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        
        System.out.println("\n\u001B[1m\u001B[32mDemo Server started at http://localhost:" + port + "/\u001B[0m\n");
        server.start();
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = getClass().getResourceAsStream("/demo/index.html")) {
                if (is == null) {
                    sendError(exchange, 404, "Not Found");
                    return;
                }
                byte[] content = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            }
        }
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            try {
                String latStr = params.get("lat");
                String lonStr = params.get("lon");
                if (latStr == null || lonStr == null) {
                    sendError(exchange, 400, "Missing lat/lon");
                    return;
                }
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                ReverseGeocodeResult result = spainGeo.reverse(lat, lon);
                
                System.out.println("Geocoding [%f, %f] -> %s".formatted(lat, lon, 
                        result != null ? result.municipality().name() : "null"));

                sendJsonResponse(exchange, result != null ? result.toJson() : "{\"result\":null}");
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid parameters: " + e.getMessage());
            }
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String query = params.get("q");
            if (query == null || query.isBlank()) {
                sendError(exchange, 400, "Missing query");
                return;
            }
            try {
                List<ReverseGeocodeResult> results = spainGeo.searchByNameContains(query);
                String json = "[" + results.stream()
                        .map(r -> r.toJson(true))
                        .reduce((acc, r) -> acc + "," + r)
                        .orElse("") + "]";
                
                sendJsonResponse(exchange, json);
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = "{\"error\":\"" + message + "\"}";
        sendJsonResponse(exchange, json);
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) result.put(entry[0], entry[1]);
        }
        return result;
    }
}
