package com.futesat.spaingeo.core;

import com.futesat.spaingeo.geo.Envelope;

import java.util.ArrayList;
import java.util.List;

public final class QuadtreeSpatialIndex {
    private final Node root;
    private final int maxItemsPerNode;
    private final int maxDepth;

    public QuadtreeSpatialIndex(Envelope bounds, int maxItemsPerNode, int maxDepth) {
        this.root = new Node(bounds, 0);
        this.maxItemsPerNode = maxItemsPerNode;
        this.maxDepth = maxDepth;
    }

    public void insert(MunicipalityFeature feature) {
        root.insert(feature);
    }

    public List<MunicipalityFeature> query(double x, double y) {
        List<MunicipalityFeature> results = new ArrayList<>();
        root.query(x, y, results);
        return results;
    }

    private final class Node {
        private final Envelope bounds;
        private final int depth;
        private final List<MunicipalityFeature> items = new ArrayList<>();
        private Node[] children;

        private Node(Envelope bounds, int depth) {
            this.bounds = bounds;
            this.depth = depth;
        }

        private void insert(MunicipalityFeature feature) {
            if (children != null) {
                int childIndex = childContaining(feature.envelope());
                if (childIndex >= 0) {
                    children[childIndex].insert(feature);
                    return;
                }
            }

            items.add(feature);

            if (children == null && items.size() > maxItemsPerNode && depth < maxDepth
                    && bounds.width() > 0 && bounds.height() > 0) {
                subdivide();
                List<MunicipalityFeature> snapshot = new ArrayList<>(items);
                items.clear();
                for (MunicipalityFeature item : snapshot) {
                    int childIndex = childContaining(item.envelope());
                    if (childIndex >= 0) {
                        children[childIndex].insert(item);
                    } else {
                        items.add(item);
                    }
                }
            }
        }

        private void query(double x, double y, List<MunicipalityFeature> results) {
            if (!bounds.contains(x, y)) {
                return;
            }
            for (MunicipalityFeature item : items) {
                if (item.envelope().contains(x, y)) {
                    results.add(item);
                }
            }
            if (children != null) {
                int childIndex = childContainingPoint(x, y);
                if (childIndex >= 0) {
                    children[childIndex].query(x, y, results);
                }
            }
        }

        private void subdivide() {
            children = new Node[4];
            double midX = (bounds.minX() + bounds.maxX()) / 2.0;
            double midY = (bounds.minY() + bounds.maxY()) / 2.0;
            children[0] = new Node(new Envelope(bounds.minX(), bounds.minY(), midX, midY), depth + 1); // SW
            children[1] = new Node(new Envelope(midX, bounds.minY(), bounds.maxX(), midY), depth + 1); // SE
            children[2] = new Node(new Envelope(bounds.minX(), midY, midX, bounds.maxY()), depth + 1); // NW
            children[3] = new Node(new Envelope(midX, midY, bounds.maxX(), bounds.maxY()), depth + 1); // NE
        }

        private int childContaining(Envelope env) {
            if (children == null) {
                return -1;
            }
            for (int i = 0; i < children.length; i++) {
                if (children[i].bounds.contains(env)) {
                    return i;
                }
            }
            return -1;
        }

        private int childContainingPoint(double x, double y) {
            if (children == null) {
                return -1;
            }
            for (int i = 0; i < children.length; i++) {
                if (children[i].bounds.contains(x, y)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
