package org.evrete.api;

import java.util.Collection;
import java.util.function.*;

/**
 * <p>
 * The definition of a type which is assigned to every object before it gets
 * into the working memory.
 * </p>
 */
public interface Type extends Named {

    Collection<TypeField> getDeclaredFields();

    /**
     * <p>
     *      Returns a declared field with the given name, or null
     *      if no such field is found.
     * </p>
     *
     * @param name field name
     * @return a declared field or null
     */
    TypeField getField(String name);

    /**
     * <p>
     *     A field declaration through a lambda expression, like "<code>o -&gt; {return o.getSomeValue()}</code>"
     *     The expression may also contain valid Java code before returning its value.
     * </p>
     *
     * @param name field name
     * @param type field value type
     * @param lambdaExpression lambda expression as a literal
     * @return newly created field
     */
    TypeField declareField(String name, Class<?> type, String lambdaExpression);

    /**
     * <p>
     *      Field declaration with a {@link Function} as value reader.
     * </p>
     * @param name field name
     * @param type field value type
     * @param function the function that will be used to read field's value
     * @param <T> expected Java type of the function's argument
     * @return newly created field
     */
    <T> TypeField declareField(String name, Class<?> type, Function<T, Object> function);

    default <T> TypeField declareField(final String name, final ToIntFunction<T> function) {
        return declareField(name, int.class, function::applyAsInt);
    }

    default <T> TypeField declareField(final String name, final ToLongFunction<T> function) {
        return declareField(name, long.class, function::applyAsLong);
    }

    default <T> TypeField declareField(final String name, final ToDoubleFunction<T> function) {
        return declareField(name, double.class, function::applyAsDouble);
    }

    default <T> TypeField declareField(final String name, final Predicate<T> function) {
        return declareField(name, boolean.class, function::test);
    }
}
