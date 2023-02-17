package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.api.TypeWrapper;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.collections.ArrayOf;

import java.util.*;
import java.util.logging.Logger;

class DefaultTypeResolver implements TypeResolver {
    private static final Logger LOGGER = Logger.getLogger(DefaultTypeResolver.class.getName());
    private final Map<String, Type<?>> typeDeclarationMap = new HashMap<>();
    private final Map<Integer, Type<?>> typesById = new HashMap<>();
    private final Map<String, ArrayOf<Type<?>>> typesByJavaType = new HashMap<>();

    private final Map<String, TypeCacheEntry> typeInheritanceCache = new HashMap<>();
    private final ClassLoader classLoader;
    private int fieldSetsCounter = 0;

    DefaultTypeResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private DefaultTypeResolver(DefaultTypeResolver other) {
        this.classLoader = other.classLoader;
        this.fieldSetsCounter = other.fieldSetsCounter;
        for (Map.Entry<String, Type<?>> entry : other.typeDeclarationMap.entrySet()) {
            Type<?> clonedType = entry.getValue().copyOf();
            this.typeDeclarationMap.put(entry.getKey(), clonedType);
            this.typesById.put(clonedType.getId(), clonedType);
        }

        for (Type<?> t : this.typeDeclarationMap.values()) {
            String javaType = t.getJavaType();
            this.typesByJavaType
                    .computeIfAbsent(javaType, s -> new ArrayOf<>(new Type<?>[0]))
                    .append(t);
        }

        for (Map.Entry<String, ArrayOf<Type<?>>> entry : other.typesByJavaType.entrySet()) {
            this.typesByJavaType.put(entry.getKey(), new ArrayOf<>(entry.getValue()));
        }
    }

    private static Class<?> primitiveClassForName(String className) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                return null;
        }
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> getType(int typeId) {
        Type<?> t = typesById.get(typeId);
        if (t == null) {
            throw new NoSuchElementException("No type found with id=" + typeId);
        } else {
            return (Type<T>) t;
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> getType(String name) {
        return (Type<T>) typeDeclarationMap.get(name);
    }


    @Override
    public synchronized void wrapType(TypeWrapper<?> typeWrapper) {
        Type<?> delegate = typeWrapper.getDelegate();
        String typeName = typeWrapper.getName();
        int typeId = typeWrapper.getId();
        Type<?> prev = this.typeDeclarationMap.put(typeName, typeWrapper);
        if (prev != delegate) {
            throw new IllegalStateException(typeWrapper + " wraps an unknown type");
        }

        prev = this.typesById.put(typeId, typeWrapper);
        if (prev != delegate) {
            throw new IllegalStateException(typeWrapper + " wraps an unknown type");
        }


        ArrayOf<Type<?>> byJavaTypes = typesByJavaType.get(typeWrapper.getJavaType());
        if (byJavaTypes == null) {
            throw new IllegalStateException();
        }

        boolean changed = false;
        for (int i = 0; i < byJavaTypes.data.length; i++) {
            if (byJavaTypes.data[i] == delegate) {
                byJavaTypes.data[i] = typeWrapper; // Replacing the type
                changed = true;
                break;
            }
        }

        if (!changed) {
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(String javaType) {
        try {
            Class<?> clazz = primitiveClassForName(javaType);
            if (clazz == null) {
                clazz = classLoader.loadClass(javaType);
            }
            return (Class<T>) clazz;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public synchronized <T> Type<T> declare(@NonNull String typeName, @NonNull String javaType) {
        return saveNewType(typeName, new TypeImpl<>(typeName, javaType, newId(), () -> {
            Class<T> resolvedJavaType = classForName(javaType);
            if (resolvedJavaType == null) {
                throw new IllegalStateException("Unable to resolve Java class '" + javaType + "'");
            } else {
                return resolvedJavaType;
            }
        }));
    }

    @Override
    @NonNull
    public synchronized <T> Type<T> declare(@NonNull String typeName, @NonNull Class<T> javaType) {
        return saveNewType(typeName, new TypeImpl<>(typeName, javaType.getName(), newId(), () -> javaType));
    }

    private int newId() {
        return typeDeclarationMap.size();
    }

    private <T> Type<T> saveNewType(String typeName, Type<T> type) {
        if (typeDeclarationMap.put(typeName, type) == null) {
            typesById.put(type.getId(), type);
            typesByJavaType.computeIfAbsent(
                            type.getJavaType(),
                            k -> new ArrayOf<Type<?>>(new Type[]{}))
                    .append(type);
            typeInheritanceCache.clear();
            return type;
        } else {
            throw new IllegalStateException("Type name '" + typeName + "' has been already defined");
        }
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return Collections.unmodifiableCollection(typeDeclarationMap.values());
    }


    private Type<?> findInSuperClasses(Class<?> type) {
        List<Type<?>> matched = new ArrayList<>(typeDeclarationMap.size());
        for (Type<?> t : typeDeclarationMap.values()) {
            if (t.resolveJavaType().isAssignableFrom(type)) {
                matched.add(t);
            }
        }

        switch (matched.size()) {
            case 0:
                return null;
            case 1:
                return matched.iterator().next();
            default:
                LOGGER.warning("Unable to resolve type '" + type + "' due to ambiguity.");
                return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> resolve(Object o) {
        Objects.requireNonNull(o);
        Class<?> javaType = o.getClass();
        String name = javaType.getName();

        ArrayOf<Type<?>> associatedTypes = typesByJavaType.get(name);
        if (associatedTypes != null) {
            if (associatedTypes.data.length > 1) {
                LOGGER.warning("Ambiguous type declaration found, there are " + associatedTypes.data.length + " types associated with '" + name + "' Java type, returning <null>.");
                return null;
            } else {
                return (Type<T>) associatedTypes.data[0];
            }
        } else {
            // There is no direct match, but there might be a registered super class that can be used instead
            TypeCacheEntry cacheEntry = typeInheritanceCache.get(name);
            if (cacheEntry == null) {
                synchronized (this) {
                    cacheEntry = typeInheritanceCache.get(name);
                    if (cacheEntry == null) {
                        cacheEntry = new TypeCacheEntry(findInSuperClasses(javaType));
                        typeInheritanceCache.put(name, cacheEntry);
                    }
                }
            }
            return (TypeImpl<T>) cacheEntry.type;
        }
    }

    @Override
    public DefaultTypeResolver copyOf() {
        return new DefaultTypeResolver(this);
    }

    private static class TypeCacheEntry {
        private final Type<?> type;

        TypeCacheEntry(Type<?> resolved) {
            this.type = resolved;
        }
    }
}
