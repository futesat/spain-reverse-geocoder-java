package com.futesat.spaingeo.model;

public record AdminDivision(String id, String name) {
    public String toJson() {
        return "{ \"id\": \"%s\", \"name\": \"%s\" }".formatted(JsonEscaper.escape(id), JsonEscaper.escape(name));
    }
}
