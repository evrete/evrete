package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;

class FactHandleImpl implements FactHandle {
    private static final long serialVersionUID = -7110831365624326343L;
    final long id;
    final int hash;
    final int type;

    FactHandleImpl(long id, int hash, int typeId) {
        this.id = id;
        this.hash = hash;
        this.type = typeId;
    }

    @Override
    public int getTypeId() {
        return this.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactHandleImpl that = (FactHandleImpl) o;
        return type == that.type && id == that.id;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                //", hash=" + hash +
                ", id=" + id +
                '}';
    }
}
