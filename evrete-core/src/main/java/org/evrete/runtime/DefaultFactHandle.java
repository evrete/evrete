package org.evrete.runtime;

import org.evrete.api.FactHandle;

import java.util.concurrent.atomic.AtomicLong;

public final class DefaultFactHandle extends PreHashed implements FactHandle {
    private static final ActiveType.Idx NULL_VALUE = new ActiveType.Idx(-1);
    private static final AtomicLong COUNTER = new AtomicLong();
    private static final long serialVersionUID = -7441124046033579340L;

    private final long id;
    private final ActiveType.Idx type;

    @SuppressWarnings("unused") // Serializable requirement
    private DefaultFactHandle() {
        this(NULL_VALUE);
    }

    DefaultFactHandle(ActiveType.Idx typeId) {
        super(typeId.getIndex());
        this.id = COUNTER.incrementAndGet();
        this.type = typeId;
    }

    ActiveType.Idx getType() {
        return type;
    }

    @Override
    public long getId() {
        return id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultFactHandle handle = (DefaultFactHandle) o;
        return id == handle.id;
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
