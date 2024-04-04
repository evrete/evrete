package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.function.*;

/**
 * <p>
 * An engine's internal type which is assigned to every fact before it gets
 * into the session's working memory. It allows for dynamic, real-time field declarations
 * either as functional interfaces or via the Java Reflection API.
 * </p>
 *
 * @param <T> Java type associated with this type
 */
//TODO logical types !!!!! this is a must
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


    Class<T> resolveJavaType();

    /**
     * <p>
     * There can be only one Type with the given name, but there could be
     * many types associated with a specific Java Class. This method returns the
     * associated Java type.
     * </p>
     *
     * @return name of the Java Class associated with the type.
     */
    String getJavaType();

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
     * Returns a declared field with the given name. If no such field was explicitly declared,
     * an attempt will be made to resolve the respective field/getter of the declared Java class.
     * If no such field or getter is found, the method will throw {@link  IllegalArgumentException}
     * </p>
     * <p>
     * Empty field name has a special meaning of the {@code "this"} value, i.e. such field's values
     * represent the type's instances themself.
     * </p>
     *
     * @param name field name or empty string if the field denotes the type's {@code this} value
     * @return declared or resolved field
     * @throws IllegalArgumentException if no field with such name could be found or resolved
     */
    @NonNull
    TypeField getField(@NonNull String name);

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
     * Method declares a primitive {@code int} field.
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
     * Method declares a primitive {@code long} field.
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
     * Method declares a primitive {@code double} field.
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
     * Method declares a primitive {@code boolean} field.
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
