package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

class DefaultTypeResolver implements TypeResolver {
    private static final Logger LOGGER = Logger.getLogger(DefaultTypeResolver.class.getName());
    private final Map<String, Type<?>> typeDeclarationMap = new HashMap<>();
    private final Map<Class<?>, Collection<Type<?>>> typesByJavaType = new HashMap<>();

    private final Map<String, TypeCacheEntry> typeInheritanceCache = new HashMap<>();
    private int fieldSetsCounter = 0;
    private final ClassLoader classLoader;

    DefaultTypeResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private DefaultTypeResolver(DefaultTypeResolver other, ClassLoader newClassLoader) {
        this.fieldSetsCounter = other.fieldSetsCounter;
        this.classLoader = newClassLoader;
        for (Map.Entry<String, Type<?>> entry : other.typeDeclarationMap.entrySet()) {
            Type<?> clonedType = entry.getValue().copyOf();
            this.typeDeclarationMap.put(entry.getKey(), clonedType);
        }

        for (Type<?> t : this.typeDeclarationMap.values()) {
            Class<?> javaType = t.getJavaClass();
            this.typesByJavaType
                    .computeIfAbsent(javaType, s -> new ArrayList<>())
                    .add(t);
        }

        for (Map.Entry<Class<?>, Collection<Type<?>>> entry : other.typesByJavaType.entrySet()) {
            this.typesByJavaType.put(entry.getKey(), new ArrayList<>(entry.getValue()));
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

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> getType(String name) {
        return (Type<T>) typeDeclarationMap.get(name);
    }


    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(ClassLoader classLoader, String javaType) {
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
        Class<T> resolvedJavaType = classForName(classLoader, javaType);
        if (resolvedJavaType == null) {
            throw new IllegalStateException("Unable to resolve Java class '" + javaType + "'");
        } else {
            return saveNewType(typeName, new TypeImpl<>(typeName, resolvedJavaType));
        }
    }

    @Override
    @NonNull
    public synchronized <T> Type<T> declare(@NonNull String typeName, @NonNull Class<T> javaType) {
        return saveNewType(typeName, new TypeImpl<>(typeName, javaType));
    }

    private <T> Type<T> saveNewType(String typeName, Type<T> type) {
        if (typeDeclarationMap.put(typeName, type) == null) {
            typesByJavaType.computeIfAbsent(
                            type.getJavaClass(),
                            k -> new ArrayList<>())
                    .add(type);
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

    @Override
    public Collection<Type<?>> getKnownTypes(Class<?> javaClass) {
        return  Collections.unmodifiableCollection(this.typesByJavaType.getOrDefault(javaClass, Collections.emptySet()));
    }

    private Type<?> findInSuperClasses(Class<?> type) {
        List<Type<?>> matched = new ArrayList<>(typeDeclarationMap.size());
        for (Type<?> t : typeDeclarationMap.values()) {
            if (t.getJavaClass().isAssignableFrom(type)) {
                matched.add(t);
            }
        }

        switch (matched.size()) {
            case 0:
                return null;
            case 1:
                return matched.iterator().next();
            default:
                LOGGER.warning(()->"Unable to resolve type '" + type + "' due to ambiguity.");
                return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Type<T> resolve(Object o) {
        Objects.requireNonNull(o);
        Class<?> javaType = o.getClass();
        String name = javaType.getName();

        Collection<Type<?>> associatedTypes = typesByJavaType.get(javaType);
        if (associatedTypes != null && !associatedTypes.isEmpty()) {
            if (associatedTypes.size() > 1) {
                LOGGER.warning(()->"Ambiguous type declaration found, there are " + associatedTypes.size() + " types associated with the '" + name + "' Java type, returning <null>.");
                return null;
            } else {
                return (Type<T>) associatedTypes.iterator().next();
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
    public DefaultTypeResolver copy(ClassLoader classLoader) {
        return new DefaultTypeResolver(this, classLoader);
    }

    private static class TypeCacheEntry {
        private final Type<?> type;

        TypeCacheEntry(Type<?> resolved) {
            this.type = resolved;
        }
    }
}
