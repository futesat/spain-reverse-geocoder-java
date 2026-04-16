package com.futesat.spaingeo;

import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.util.Random;

/**
 * Simple benchmark for the Spain Reverse Geocoder.
 * Measures initialization time and lookup throughput.
 * No external dependencies required.
 */
public final class Benchmark {
    private static final int WARMUP_ITERATIONS = 5000;
    private static final int MEASURE_ITERATIONS = 50000;
    
    // Spain bounding box (approximate)
    private static final double MIN_LAT = 36.0;
    private static final double MAX_LAT = 43.8;
    private static final double MIN_LON = -9.3;
    private static final double MAX_LON = 3.3;

    public static void main(String[] args) {
        System.out.println("=== Spain Reverse Geocoder Benchmark ===\n");

        // 1. Measure Initialization
        long startInit = System.nanoTime();
        SpainGeo geo = SpainGeo.builder().build();
        long endInit = System.nanoTime();
        
        double initMs = (endInit - startInit) / 1_000_000.0;
        System.out.printf("Initialization: %.2f ms%n", initMs);
        System.out.printf("Municipalities loaded: %d%n%n", geo.size());

        // 2. Warm-up
        System.out.println("Warming up...");
        runLookups(geo, WARMUP_ITERATIONS);

        // 3. Measure Lookups
        System.out.println("Measuring throughput (" + MEASURE_ITERATIONS + " iterations)...");
        long startMeasure = System.nanoTime();
        int found = runLookups(geo, MEASURE_ITERATIONS);
        long endMeasure = System.nanoTime();

        double totalS = (endMeasure - startMeasure) / 1_000_000_000.0;
        double throughput = MEASURE_ITERATIONS / totalS;
        double avgMs = (totalS * 1000.0) / MEASURE_ITERATIONS;

        System.out.println("\n--- Results ---");
        System.out.printf("Total time:   %.2f s%n", totalS);
        System.out.printf("Throughput:   %.0f lookups/s%n", throughput);
        System.out.printf("Avg latency:  %.4f ms/lookup%n", avgMs);
        System.out.printf("Found match:  %d/%d (%.1f%%)%n", found, MEASURE_ITERATIONS, (found * 100.0 / MEASURE_ITERATIONS));
        
        // Memory estimate (rough)
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Memory used:  ~%d MB%n", usedMemory / (1024 * 1024));
    }

    private static int runLookups(SpainGeo geo, int count) {
        Random rnd = new Random(42); // Use fixed seed for reproducibility
        int foundCount = 0;
        for (int i = 0; i < count; i++) {
            double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * rnd.nextDouble();
            double lon = MIN_LON + (MAX_LON - MIN_LON) * rnd.nextDouble();
            ReverseGeocodeResult result = geo.reverse(lat, lon);
            if (result != null) {
                foundCount++;
            }
        }
        return foundCount;
    }
}
