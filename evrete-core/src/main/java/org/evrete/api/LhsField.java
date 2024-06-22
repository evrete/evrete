package org.evrete.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A convenient generic class for referencing LHS (Left-Hand Side) field declarations.
 * During different lifecycle stages of rules, the engine uses various (often temporary) representations
 * of field references, and this class provides a convenient means of transforming these references.
 *
 * @param <Fact>  the type of the referenced fact
 * @param <Field> the type of the referenced field
 */
public class LhsField<Fact, Field> {
    private final Fact fact;
    private final Field field;

    public LhsField(Fact fact, Field field) {
        this.fact = fact;
        this.field = field;
    }

    public LhsField(LhsField<Fact, ?> other, Field field) {
        this(other.fact, field);
    }

    public LhsField(Fact fact, LhsField<?, Field> other) {
        this(fact, other.field);
    }

    public Fact fact() {
        return fact;
    }

    public Field field() {
        return field;
    }

    public <Fact1, Field1> LhsField<Fact1, Field1> transform(Function<Fact, Fact1> factMapper, Function<Field, Field1> fieldMapper) {
        return new LhsField<>(factMapper.apply(this.fact), fieldMapper.apply(this.field));
    }

    public static LhsField<String, String> parseDottedVariable(String arg) {
        int dotPos = arg.indexOf('.');
        if (dotPos < 0) {
            // The matched expression refers to a fact itself
            return new LhsField<>(arg, (String) null);
        } else {
            String factName = arg.substring(0, dotPos);
            String fieldName = arg.substring(dotPos + 1);
            return new LhsField<>(factName, fieldName);
        }
    }

    @Override
    public String toString() {
        return "{fact=" + fact +
                ", field=" + field +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LhsField<?, ?> lhsField = (LhsField<?, ?>) o;
        return Objects.equals(fact, lhsField.fact) && Objects.equals(field, lhsField.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fact, field);
    }

    /**
     * In many cases, the order of field references matters, and this class provides a wrapper for an array
     * of references.
     *
     * @param <Fact>  the type of the referenced fact
     * @param <Field> the type of the referenced field
     */
    public static final class Array<Fact, Field> {
        private final LhsField<Fact, Field>[] fields;

        public Array(LhsField<Fact, Field>[] fields) {
            this.fields = fields;
        }

        @SuppressWarnings("unchecked")
        public Array(List<LhsField<Fact, Field>> arg) {
            this.fields = (LhsField<Fact, Field>[]) new LhsField<?,?>[arg.size()];
            int i = 0;
            for(LhsField<Fact, Field> f : arg) {
                this.fields[i++] = f;
            }
        }

        public LhsField<Fact, Field> get(int index) {
            return fields[index];
        }

        public int length() {
            return fields.length;
        }

        @SuppressWarnings("unchecked")
        public <Fact1, Field1> Array<Fact1, Field1> transform(Function<LhsField<Fact, Field>, LhsField<Fact1, Field1>> factMapper) {
            LhsField<Fact1, Field1>[] newFields = (LhsField<Fact1, Field1>[]) new LhsField<?, ?>[fields.length];
            for (int i = 0; i < fields.length; i++) {
                newFields[i] = factMapper.apply(this.fields[i]);
            }
            return new Array<>(newFields);
        }

        @Override
        public String toString() {
            return Arrays.toString(fields);
        }

        public static LhsField.Array<String, String> fromDottedVariables(String[] fieldNames) {
            List<LhsField<String, String>> list = new ArrayList<>(fieldNames.length);
            for (String fieldName : fieldNames) {
                LhsField<String, String> parsed = LhsField.parseDottedVariable(fieldName);
                list.add(parsed);
            }
            return new LhsField.Array<>(list);
        }
    }
}
