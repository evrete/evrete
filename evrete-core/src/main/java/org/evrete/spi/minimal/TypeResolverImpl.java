package org.evrete.spi.minimal;

import org.evrete.api.Type;
import org.evrete.api.TypeResolver;

import java.util.*;
import java.util.logging.Logger;

class TypeResolverImpl implements TypeResolver {
    private static final List<Class<?>> EMPTY_CLASS_LIST = new ArrayList<>();
    private final Map<String, TypeImpl> typeDeclarationMap = new HashMap<>();
    private final Map<String, TypeCacheEntry> typeInheritanceCache = new HashMap<>();
    private final JcCompiler compiler;
    private int typeCounter = 0;
    private int fieldSetsCounter = 0;

    public TypeResolverImpl(JcCompiler compiler) {
        this.compiler = compiler;
    }

    private TypeResolverImpl(TypeResolverImpl other) {
        this.compiler = other.compiler;
        this.typeCounter = other.typeCounter;
        this.fieldSetsCounter = other.fieldSetsCounter;
        for (Map.Entry<String, TypeImpl> entry : other.typeDeclarationMap.entrySet()) {
            this.typeDeclarationMap.put(entry.getKey(), entry.getValue().copyOf());
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

    @Override
    public Collection<Type> getKnownTypes() {
        return Collections.unmodifiableCollection(typeDeclarationMap.values());
    }

    @Override
    public TypeImpl getType(String name) {
        return typeDeclarationMap.get(name);
    }


    private TypeImpl findInSuperClasses(String name) {
        Class<?> clazz;
        List<TypeImpl> matching = new LinkedList<>();
        for (TypeImpl existing : typeDeclarationMap.values()) {
            if ((clazz = existing.getClazz()) != null) {
                List<Class<?>> superClasses = superClasses(clazz);
                for (Class<?> sup : superClasses) {
                    if (sup.getName().equals(name)) {
                        matching.add(existing);
                    }
                }
            }
        }

        switch (matching.size()) {
            case 0:
                return null;
            case 1:
                return matching.iterator().next();
            default:
                Logger.getAnonymousLogger().warning("Unable to resolve type '" + name + "' due to ambiguity.");
                return null;
        }
    }

    @Override
    public TypeImpl resolve(Object o) {
        String name = o.getClass().getName();
        TypeImpl found = typeDeclarationMap.get(name);

        if (found == null) {
            // No directly declared types, looking is super classes
            synchronized (this) {
                TypeCacheEntry cacheEntry = typeInheritanceCache.get(name);
                if (cacheEntry == null) {
                    cacheEntry = new TypeCacheEntry(findInSuperClasses(name));
                    typeInheritanceCache.put(name, cacheEntry);
                }
                found = cacheEntry.type; // Can be null
            }
        }
        return found;
    }

    @Override
    public TypeResolverImpl copyOf() {
        return new TypeResolverImpl(this);
    }

    @Override
    public final synchronized TypeImpl declare(String typeName) {
        TypeImpl type = typeDeclarationMap.get(typeName);
        if (type == null) {
            type = TypeImpl.factory(compiler, typeName);
            typeDeclarationMap.put(typeName, type);
            if (type.getClazz() != null) {
                // Clear the inheritance cache
                this.typeInheritanceCache.clear();
            }
            return type;
        } else {
            throw new IllegalArgumentException("Type named '" + typeName + "' has been already declared");
        }
    }

    private static class TypeCacheEntry {
        private final TypeImpl type;

        TypeCacheEntry(TypeImpl resolved) {
            this.type = resolved;
        }
    }
}
