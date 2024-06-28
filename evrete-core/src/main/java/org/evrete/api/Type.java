package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.function.*;

/**
 * Represents an engine's internal logical type, which is assigned to every fact before
 * it is inserted into the session's working memory. A fact's association with a logical type
 * can be explicit, via the {@link RuleSession#insertAs(String, Object)} method, or implicit,
 * through the fact's Java type.
 * <p>
 * Each logical type is uniquely identified by its logical name, retrievable via the {@link Type#getName()}
 * method, and is associated with a specific Java type, obtainable through the {@link Type#getJavaClass()} method.
 * This mechanism allows for facts of the same Java type to be differentiated based on their logical names.
 * </p>
 * <p>
 * Each type inherits public fields and getters from its associated Java class, while also supporting
 * the declaration of custom fields for use in rule conditions.
 * </p>
 *
 * @param <T> the Java type associated with this logical type
 */

public interface Type<T> extends Copyable<Type<T>> {


    /**
     * Returns logical name of the type
     * @return logical name
     */
    String getName();

    /**
     *
     * @return the resolved Java Class associated with the type.
     * @deprecated use the {@link #getJavaClass()} method instead
     */
    @Deprecated
    Class<T> resolveJavaType();

    /**
     * <p>
     * There can be only one Type with the given name, but there could be
     * many types associated with a specific Java Class. This method returns the
     * associated Java type.
     * </p>
     *
     * @return the Java Class associated with the type.
     */
    Class<T> getJavaClass();

    /**
     * <p>
     * There can be only one Type with the given name, but there could be
     * many types associated with a specific Java Class. This method returns the
     * associated Java type.
     * </p>
     *
     * @return name of the Java Class associated with the type.
     * @deprecated use the {@link #getJavaClass()} method instead
     */
    @Deprecated
    String getJavaType();


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

    static String logicalNameOf(@NonNull Class<?> cl) {
        return cl.getName();
    }
}
