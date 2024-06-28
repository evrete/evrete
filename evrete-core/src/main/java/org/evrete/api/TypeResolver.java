package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.TypeWrapper;

import java.util.Collection;

/**
 * <p>
 * TypeResolver provides dynamic mapping of Java types to the engine's internal {@link Type}.
 * In the engine, all fact types are represented by a String identifier and an associated Java
 * class. This allows instances of the same Java class to be treated as having different logical types.
 * </p>
 *
 * <p>
 * This interface extends {@link CopyableWith}, allowing a {@link TypeResolver} to create copies of itself.
 * The copies must inherit the original declared types but must not have a reverse effect — changes,
 * such as new types or field declarations, in the copies must not be reflected in the original type resolver.
 * </p>
 *
 * @see CopyableWith
 */
//TODO remove the classloader in copyable
public interface TypeResolver extends CopyableWith<TypeResolver, ClassLoader> {

    /**
     * @param name type's logical name
     * @param <T>  type parameter
     * @return existing {@link Type} or {@code null} if not found
     */
    @Nullable
    <T> Type<T> getType(String name);


    /**
     * Returns a collection of all known types.
     *
     * @return a collection of Type instances representing the known types
     */
    Collection<Type<?>> getKnownTypes();

    /**
     * Returns a collection of all known types that have the same Java type as specified by the argument.
     *
     * @param javaClass the class of the Java type to find known types for
     * @return a collection of Type instances representing the known types with the specified Java type
     */
    Collection<Type<?>> getKnownTypes(Class<?> javaClass);

    /**
     * Wraps a given TypeWrapper instance and delegates the calls to another Type implementation.
     *
     * @param typeWrapper the TypeWrapper instance to be wrapped
     * @deprecated in 4.0.0 use the {@link #addType(Type)} method instead
     * to supply an alternative type resolver
     */
    @Deprecated
    default void wrapType(TypeWrapper<?> typeWrapper) {
        throw new UnsupportedOperationException("Deprecated in 4.0.0");
    }

    /**
     * Adds a new type to the type resolver and overrides any previous association
     * between the type's logical name and the type itself.
     *
     * @param type The type to be added to the resolver.
     * @since 4.0.0
     */
    void addType(Type<?> type);

    /**
     * <p>
     * Declares and registers new {@link Type} with the given Java class name.
     * The logical name of the resulting type will be {@link Class#getName()}
     * </p>
     *
     * @param type Java class
     * @param <T>  java class type parameter
     * @return new internal type
     * @throws IllegalStateException if such type name has been already declared
     */
    default <T> Type<T> declare(@NonNull Class<T> type) {
        return declare(Type.logicalNameOf(type), type);
    }

    /**
     * <p>
     * Declares and registers new {@link Type} with the given type name and Java class
     * </p>
     *
     * @param typeName logical type name
     * @param javaType Java class
     * @param <T>      java class type parameter
     * @return new internal type
     * @throws IllegalStateException if such type name has been already declared
     */
    @NonNull
    <T> Type<T> declare(@NonNull String typeName, @NonNull Class<T> javaType);

    /**
     * <p>
     * Declares and registers new {@link Type} with the given logical type name and Java class name.
     * The existence of the corresponding Java class will be checked lazily, when the engine
     * requires access to the class's properties.
     * </p>
     *
     * @param typeName logical type name
     * @param javaType Java class name
     * @param <T>      java class type parameter
     * @return new logical type
     * @throws IllegalStateException if such type name has been already declared
     */
    @NonNull
    <T> Type<T> declare(@NonNull String typeName, @NonNull String javaType);

    @NonNull
    default <T> Type<T> getOrDeclare(String typeName, Class<T> javaType) {
        Type<T> t = getType(typeName);
        if (t == null) {
            t = declare(typeName, javaType);
        }
        return t;
    }

    @NonNull
    default <T> Type<T> getOrDeclare(String typeName, String javaType) {
        Type<T> t = getType(typeName);
        if (t == null) {
            t = declare(typeName, javaType);
        }
        return t;
    }

    default <T> Type<T> getOrDeclare(String typeName) {
        return getOrDeclare(typeName, typeName);
    }

    default <T> Type<T> getOrDeclare(Class<T> cl) {
        return getOrDeclare(cl.getName(), cl);
    }


    /**
     * @param o   object to resolve
     * @param <T> type parameter
     * @return Type of the argument or null if object's type is unknown
     */
    <T> Type<T> resolve(Object o);

}
