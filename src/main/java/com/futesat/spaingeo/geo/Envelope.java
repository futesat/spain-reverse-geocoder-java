package com.futesat.spaingeo.geo;

public final class Envelope {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public Envelope(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double minX() { return minX; }
    public double minY() { return minY; }
    public double maxX() { return maxX; }
    public double maxY() { return maxY; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Envelope envelope = (Envelope) o;
        return Double.compare(envelope.minX, minX) == 0 && Double.compare(envelope.minY, minY) == 0 &&
               Double.compare(envelope.maxX, maxX) == 0 && Double.compare(envelope.maxY, maxY) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(minX, minY, maxX, maxY);
    }

    @Override
    public String toString() {
        return "Envelope{minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY + "}";
    }
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
