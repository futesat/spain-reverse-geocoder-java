package com.futesat.spaingeo.model;

public final class AdminDivision {
    private final String id;
    private final String name;

    public AdminDivision(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }
    public String name() { return name; }

    public String toJson() {
        return String.format("{ \"id\": \"%s\", \"name\": \"%s\" }", JsonEscaper.escape(id), JsonEscaper.escape(name));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminDivision that = (AdminDivision) o;
        return java.util.Objects.equals(id, that.id) && java.util.Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "AdminDivision{id='" + id + "', name='" + name + "'}";
    }
}
