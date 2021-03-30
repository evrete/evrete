package org.evrete.api;

import java.util.Collection;
import java.util.function.*;

/**
 * <p>
 * An engine's internal type which is assigned to every fact before it gets
 * into the session's working memory. It allows for dynamic, real-time field declarations
 * either as functional interfaces or via reflection mechanisms.
 * </p>
 *
 * @param <T> Java type associated with this type
 */
public interface Type<T> extends Named, Copyable<Type<T>> {

    /**
     * <p>
     * Each type is assigned a unique auto-increment int identifier which developers can use in SPI implementations,
     * for example in sharding/partitioning data collections.
     * </p>
     *
     * @return unique type identifier.
     */
    int getId();

    /**
     * <p>
     * There can be only one {@link Type} with the given name, but there could be
     * many types associated with a specific Java Class. This method returns the
     * associated Java type.
     * </p>
     *
     * @return Java Class associated with the type.
     */
    Class<T> getJavaType();

    /**
     * <p>
     * Method returns ALL known fields, both explicitly declared and resolved.
     * </p>
     *
     * @return Collection of declared fields
     * @see TypeField
     */
    Collection<TypeField> getDeclaredFields();

    /**
     * <p>
     * Returns a declared field with the given name, or null
     * if no such field is found or resolved
     * </p>
     *
     * @param name field name
     * @return a declared field or null
     */
    TypeField getField(String name);


    /**
     * <p>
     * Field declaration with a {@link Function} as value reader.
     * </p>
     *
     * @param name     field name
     * @param type     field value class
     * @param <V>      field value generic type
     * @param function the function that will be used to read field's value
     * @return a newly created field or an existing one if already declared/resolved
     */
    <V> TypeField declareField(String name, Class<V> type, Function<T, V> function);

    /**
     * <p>
     * Method declares a primitive <code>int</code> field.
     * </p>
     *
     * @param name     field name
     * @param function field functional interface
     * @return a newly created field or an existing one if already declared/resolved
     */
    default TypeField declareIntField(final String name, final ToIntFunction<T> function) {
        return declareField(name, int.class, function::applyAsInt);
    }

    /**
     * <p>
     * Method declares a primitive <code>long</code> field.
     * </p>
     *
     * @param name     field name
     * @param function field functional interface
     * @return a newly created field or an existing one if already declared/resolved
     */
    default TypeField declareLongField(final String name, final ToLongFunction<T> function) {
        return declareField(name, long.class, function::applyAsLong);
    }

    /**
     * <p>
     * Method declares a primitive <code>double</code> field.
     * </p>
     *
     * @param name     field name
     * @param function field functional interface
     * @return a newly created field or an existing one if already declared/resolved
     */
    default TypeField declareDoubleField(final String name, final ToDoubleFunction<T> function) {
        return declareField(name, double.class, function::applyAsDouble);
    }

    /**
     * <p>
     * Method declares a primitive <code>boolean</code> field.
     * </p>
     *
     * @param name     field name
     * @param function field functional interface
     * @return a newly created field or an existing one if already declared/resolved
     */
    default TypeField declareBooleanField(final String name, final Predicate<T> function) {
        return declareField(name, boolean.class, function::test);
    }
}
