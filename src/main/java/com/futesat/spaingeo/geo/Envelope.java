package com.futesat.spaingeo.geo;

public record Envelope(double minX, double minY, double maxX, double maxY) {
    public static Envelope fromPoint(double x, double y) {
        return new Envelope(x, y, x, y);
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public boolean contains(Envelope other) {
        return other.minX >= minX && other.maxX <= maxX && other.minY >= minY && other.maxY <= maxY;
    }

    public boolean intersects(Envelope other) {
        return !(other.maxX < minX || other.minX > maxX || other.maxY < minY || other.minY > maxY);
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }

    public Envelope expandToInclude(double x, double y) {
        return new Envelope(
                Math.min(minX, x),
                Math.min(minY, y),
                Math.max(maxX, x),
                Math.max(maxY, y)
        );
    }

    public Envelope expandToInclude(Envelope other) {
        return new Envelope(
                Math.min(minX, other.minX),
                Math.min(minY, other.minY),
                Math.max(maxX, other.maxX),
                Math.max(maxY, other.maxY)
        );
    }

    public Coordinate center() {
        return new Coordinate((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }
}
