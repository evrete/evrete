package org.evrete.api;

public final class FactBuilder {
    private final String name;
    private final String type;

    private FactBuilder(String name, String type) {
        this.name = name;
        this.type = type;
    }


    public static FactBuilder fact(String name, String type) {
        return new FactBuilder(name, type);
    }

    public static FactBuilder fact(String name, Class<?> type) {
        return new FactBuilder(name, type.getName());
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
