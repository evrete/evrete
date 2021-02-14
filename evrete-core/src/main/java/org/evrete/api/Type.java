package org.evrete.api;

import java.util.Collection;
import java.util.function.*;

/**
 * <p>
 * The definition of a type which is assigned to every object before it gets
 * into the working memory.
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

    Class<T> getJavaType();

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
     * @param type     field value type
     * @param function the function that will be used to read field's value
     * @return newly created field
     */
    TypeField declareField(String name, Class<?> type, Function<T, Object> function);

    default TypeField declareField(final String name, final ToIntFunction<T> function) {
        return declareField(name, int.class, function::applyAsInt);
    }

    default TypeField declareField(final String name, final ToLongFunction<T> function) {
        return declareField(name, long.class, function::applyAsLong);
    }

    default TypeField declareField(final String name, final ToDoubleFunction<T> function) {
        return declareField(name, double.class, function::applyAsDouble);
    }

    default TypeField declareField(final String name, final Predicate<T> function) {
        return declareField(name, boolean.class, function::test);
    }
}
