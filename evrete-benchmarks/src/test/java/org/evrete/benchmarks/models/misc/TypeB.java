package org.evrete.benchmarks.models.misc;

@SuppressWarnings("unused")
public class TypeB extends Base {
    public TypeB(String id) {
        super(id);
    }

    public TypeB(int i) {
        super(i);
    }

    public TypeB() {
    }

    @Override
    public String toString() {
        return "TypeB{" +
                "id='" + getId() +
                "', i=" + i +
                '}';
    }

}
