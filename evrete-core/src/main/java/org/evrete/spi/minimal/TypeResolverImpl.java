package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.collections.ArrayOf;

import java.util.*;
import java.util.logging.Logger;

class TypeResolverImpl implements TypeResolver {
    private static final Logger LOGGER = Logger.getLogger(TypeResolverImpl.class.getName());
    private static final List<Class<?>> EMPTY_CLASS_LIST = new ArrayList<>();
    private final Map<String, TypeImpl<?>> typeDeclarationMap = new HashMap<>();
    private final Map<String, ArrayOf<TypeImpl<?>>> typesByJavaType = new HashMap<>();

    private final Map<String, TypeCacheEntry> typeInheritanceCache = new HashMap<>();
    private final JcCompiler compiler;
    private int typeCounter = 0;
    private int fieldSetsCounter = 0;
    private final ClassLoader classLoader;

    public TypeResolverImpl(RuntimeContext<?> requester) {
        this.classLoader = requester.getClassLoader();
        this.compiler = new JcCompiler(classLoader);
    }

    private TypeResolverImpl(TypeResolverImpl other) {
        this.compiler = other.compiler;
        this.classLoader = other.classLoader;
        this.typeCounter = other.typeCounter;
        this.fieldSetsCounter = other.fieldSetsCounter;
        for (Map.Entry<String, TypeImpl<?>> entry : other.typeDeclarationMap.entrySet()) {
            this.typeDeclarationMap.put(entry.getKey(), entry.getValue().copyOf());
        }

        for (Map.Entry<String, ArrayOf<TypeImpl<?>>> entry : other.typesByJavaType.entrySet()) {
            this.typesByJavaType.put(entry.getKey(), new ArrayOf<>(entry.getValue()));
        }


    }

    //TODO scan interfaces as well
    private static List<Class<?>> superClasses(Class<?> subject) {
        if (subject.isArray() || subject.isPrimitive() || subject.equals(Object.class)) return EMPTY_CLASS_LIST;

        List<Class<?>> l = new ArrayList<>();

        Class<?> current = subject.getSuperclass();
        while (!current.equals(Object.class)) {
            l.add(current);
            current = current.getSuperclass();
        }

        return l;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> classForName(String javaType) {
        try {
            return (Class<T>) Class.forName(javaType, true, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public synchronized <T> Type<T> declare(String typeName, String javaType) {
        TypeImpl<T> type = getType(typeName);
        if (type != null)
            throw new IllegalStateException("Type name '" + typeName + "' has been already defined: " + type);
        Class<T> resolvedJavaType = classForName(javaType);
        if (resolvedJavaType == null)
            throw new IllegalStateException("Unable to resolve Java class name '" + javaType + "'");
        return saveNewType(typeName, new TypeImpl<>(typeName, resolvedJavaType, compiler));
    }

    @Override
    public synchronized <T> Type<T> declare(String typeName, Class<T> javaType) {
        TypeImpl<T> type = getType(typeName);
        if (type != null)
            throw new IllegalStateException("Type name '" + typeName + "' has been already defined: " + type);
        return saveNewType(typeName, new TypeImpl<>(typeName, javaType, compiler));
    }

    private <T> TypeImpl<T> saveNewType(String typeName, TypeImpl<T> type) {
        typeDeclarationMap.put(typeName, type);
        typesByJavaType.computeIfAbsent(
                type.getJavaType().getName(),
                k -> new ArrayOf<TypeImpl<?>>(new TypeImpl[]{}))
                .append(type);
        typeInheritanceCache.clear();
        return type;
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return Collections.unmodifiableCollection(typeDeclarationMap.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeImpl<T> getType(String name) {
        return (TypeImpl<T>) typeDeclarationMap.get(name);
    }

    private TypeImpl<?> findInSuperClasses(Class<?> type) {

        List<TypeImpl<?>> matching = new LinkedList<>();
        List<Class<?>> superClasses = superClasses(type);
        for(Class<?> sup : superClasses) {
            String supName = sup.getName();
            ArrayOf<TypeImpl<?>> match = typesByJavaType.get(supName);
            if(match != null && match.data.length == 1) {
                matching.add(match.data[0]);
            }
        }

        switch (matching.size()) {
            case 0:
                return null;
            case 1:
                return matching.iterator().next();
            default:
                LOGGER.warning("Unable to resolve type '" + type + "' due to ambiguity.");
                return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeImpl<T> resolve(Object o) {
        Class<?> javaType = o.getClass();
        String name = javaType.getName();

        ArrayOf<TypeImpl<?>> associatedTypes = typesByJavaType.get(name);
        if (associatedTypes != null) {
            if (associatedTypes.data.length > 1) {
                LOGGER.warning("Ambiguous type declaration found, there are " + associatedTypes.data.length + " types associated with '" + name + "' Java type, returning <null>.");
                return null;
            } else {
                return (TypeImpl<T>) associatedTypes.data[0];
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
    public TypeResolverImpl copyOf() {
        return new TypeResolverImpl(this);
    }

    private static class TypeCacheEntry {
        private final TypeImpl<?> type;

        TypeCacheEntry(TypeImpl<?> resolved) {
            this.type = resolved;
        }
    }
}
