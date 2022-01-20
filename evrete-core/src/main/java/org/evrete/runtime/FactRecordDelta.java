package org.evrete.runtime;

public final class FactRecordDelta {
    private final FactRecord previous;
    private final FactRecord latest;

    private FactRecordDelta(FactRecord previous, FactRecord latest) {
        this.previous = previous;
        this.latest = latest;
    }

    public FactRecord getPrevious() {
        return previous;
    }

    public FactRecord getLatest() {
        return latest;
    }

    static FactRecordDelta insertDelta(FactRecord record) {
        return new FactRecordDelta(null, record);
    }

    static FactRecordDelta deleteDelta(FactRecord record) {
        return new FactRecordDelta(record, null);
    }

    static FactRecordDelta updateDelta(FactRecord previous, Object newValue) {
        return new FactRecordDelta(previous, FactRecord.updated(previous, newValue));
    }

    @Override
    public String toString() {
        return "{" +
                "previous=" + previous +
                ", latest=" + latest +
                '}';
    }
}
