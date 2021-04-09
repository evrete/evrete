package org.evrete.runtime;

class FactRecord {
    final Object instance;
    private int version = 0;

    FactRecord(Object instance) {
        this.instance = instance;
    }

    public int getVersion() {
        return version;
    }

    void updateVersion(int newVersion) {
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "{obj=" + instance +
                ", ver=" + version +
                '}';
    }
}
