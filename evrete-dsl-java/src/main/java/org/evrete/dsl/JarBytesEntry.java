package org.evrete.dsl;

class JarBytesEntry {
    final String name;
    final byte[] bytes;

    public JarBytesEntry(String name, byte[] bytes) {
        this.name = name;
        this.bytes = bytes;
    }
}
