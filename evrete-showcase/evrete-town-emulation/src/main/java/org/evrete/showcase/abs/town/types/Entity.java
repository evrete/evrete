package org.evrete.showcase.abs.town.types;

import com.google.gson.GsonBuilder;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.TypeWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class Entity {
    public final String type;
    private final Map<String, Boolean> flags = new HashMap<>();
    private final Map<String, Integer> numbers = new HashMap<>();
    private final Map<String, Entity> properties = new HashMap<>();

    public Entity(String type) {
        this.type = type;
    }

    public int getNumber(String name, int defaultValue) {
        return numbers.getOrDefault(name, defaultValue);
    }

    public boolean getFlag(String name, boolean defaultValue) {
        return flags.getOrDefault(name, defaultValue);
    }

    public Entity getProperty(String name) {
        return properties.get(name);
    }

    public void set(String name, boolean flag) {
        this.flags.put(name, flag);
    }

    public void set(String name, int number) {
        this.numbers.put(name, number);
    }

    public void set(String name, Entity property) {
        if (property != null && property.getClass().isPrimitive()) {
            throw new IllegalArgumentException("Primitive types are not allowed");
        } else {
            this.properties.put(name, property);
        }
    }


    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
/*
        return "{type='" + type + '\'' +
                ", flags=" + flags +
                ", numbers=" + numbers +
                ", properties=" + properties +
                '}';
*/
    }

    public static class EntityKnowledgeType extends TypeWrapper<Entity> {
        public EntityKnowledgeType(Type<Entity> delegate) {
            super(delegate);
        }

        @Override
        public TypeField getField(String name) {
            TypeField field = super.getField(name);
            if (field != null) return field;


            String[] parts = name.split("\\.");
            if (parts.length != 2) {
                return null;
            }

            String key = parts[1];
            switch (parts[0]) {
                case "flags":
                    return declareField(name, (Predicate<Entity>) entity -> entity.getFlag(key, false));
                case "numbers":
                    return declareField(name, (ToIntFunction<Entity>) entity -> entity.getNumber(key, 0));
                case "properties":
                    return declareField(name, Entity.class, entity -> entity.getProperty(key));
                default:
                    throw new IllegalStateException();

            }
        }
    }

}
