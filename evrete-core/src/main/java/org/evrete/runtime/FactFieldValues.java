package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;

import java.util.Arrays;

public final class FactFieldValues extends PreHashed{
    private final Object[] values;

    public FactFieldValues(Object[] values) {
        super(Arrays.hashCode(values));
        this.values = values;
    }

    public int size() {
        return values.length;
    }

    public Object valueAt(int index) {
        return values[index];
    }

    private boolean equalsTo(FactFieldValues that) {
        return Arrays.equals(values, that.values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return equalsTo((FactFieldValues) o);
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    public static class Scoped {
        private final MemoryScope scope;
        private final long values;

        public Scoped(long values, MemoryScope scope) {
            this.scope = scope;
            this.values = values;
        }

        public Scoped toScope(MemoryScope scope) {
            if(scope == this.scope) {
                return this;
            } else {
                return new Scoped(values, scope);
            }
        }

        public long values() {
            return values;
        }

        public MemoryScope scope() {
            return scope;
        }

        @Override
        public String toString() {
            return "{" +
                    "scope=" + scope +
                    ", values=" + values +
                    '}';
        }
    }

}
