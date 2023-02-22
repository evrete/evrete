package org.evrete.api;

/**
 * <p>
 * Field references go along with rule conditions and are the key source of knowledge for building
 * the Rete memory graph. Each field reference consists of a {@link NamedType} and the type's
 * property/field {@link TypeField}. For example, the following condition
 * </p>
 * <pre>{@code .where("$a.id > $b.type")}</pre>
 * <p>
 * contains two field references: one is referring the <code>id</code> property in a named type
 * <code>$a</code>, and the other is related to the field <code>type</code> in a named type <code>$b</code>.
 * </p>
 * <p>
 * If condition is a string literal, the engine will consult with {@link org.evrete.api.spi.ExpressionResolverProvider}
 * to determine the condition's field references. If you decide to use Java's {@link java.util.function.Predicate}
 * or the library's {@link ValuesPredicate} as a condition, then the corresponding field references must be
 * explicitly specified like in {@link LhsBuilder#where(ValuesPredicate, String...)} or in
 * {@link LhsBuilder#where(ValuesPredicate, FieldReference...)}
 * </p>
 * <p>
 * If you decide to use functional interfaces instead of literal conditions, then using String arrays is the most
 * convenient form of providing field references:
 * </p>
 * <pre>{@code
 * .where(predicate, "$a.id", "$b.type")
 * }</pre>
 * <p>
 * In that case, the engine will consult with the rule's declared fact types and automatically resolve
 * these two string references into a {@link FieldReference} array.
 * </p>
 */
public interface FieldReference {
    FieldReference[] ZERO_ARRAY = new FieldReference[0];

    static boolean sameAs(FieldReference[] refs1, FieldReference[] refs2) {
        if (refs1.length != refs2.length) return false;

        for (int i = 0; i < refs1.length; i++) {
            if (!refs1[i].sameAs(refs2[i])) {
                return false;
            }
        }
        return true;
    }

    TypeField field();

    NamedType type();

    default boolean sameAs(FieldReference other) {
        return other.field().getName().equals(field().getName()) && other.type().sameAs(type());
    }
}
