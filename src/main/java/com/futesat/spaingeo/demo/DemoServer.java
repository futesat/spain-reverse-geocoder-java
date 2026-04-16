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
import java.util.Scanner;

/**
 * A simple, zero-dependency web server to demonstrate the geocoder.
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
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        System.out.println("\n\u001B[1m\u001B[32mDemo Server started at http://localhost:" + port + "/\u001B[0m\n");
        server.start();
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream is = getClass().getResourceAsStream("/demo/index.html");
            if (is == null) {
                String error = "404 Not Found";
                exchange.sendResponseHeaders(404, error.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(error.getBytes()); }
                return;
            }

            byte[] content;
            try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
                content = s.next().getBytes(StandardCharsets.UTF_8);
            }
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String query = params.get("q");

            if (query == null || query.trim().isEmpty()) {
                sendError(exchange, 400, "Missing 'q' parameter");
                return;
            }
            try {
                List<ReverseGeocodeResult> results = spainGeo.searchByNameContains(query);
                
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < results.size(); i++) {
                    sb.append(results.get(i).toJson(true));
                    if (i < results.size() - 1) sb.append(",");
                }
                sb.append("]");
                
                byte[] response = sb.toString().getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (Exception e) {
                System.err.println("Search API Error: " + e.getMessage());
                sendError(exchange, 500, "Internal error: " + e.getMessage());
            }
        }
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String latStr = params.get("lat");
            String lonStr = params.get("lon");

            if (latStr == null || lonStr == null) {
                sendError(exchange, 400, "Missing lat/lon parameters");
                return;
            }
            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                ReverseGeocodeResult result = spainGeo.reverse(lat, lon);
                System.out.println("Geocoding [" + lat + ", " + lon + "] -> " + (result != null ? result.municipality().name() : "null"));

                String json = result != null ? result.toJson() : "{\"result\":null}";
                byte[] response = json.getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (Exception e) {
                System.err.println("API Error: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, 500, "Internal error: " + e.getMessage());
            }
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = "{\"error\":\"" + message + "\"}";
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }
}
