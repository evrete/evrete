package org.evrete.runtime.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassMetaTest {

    @Test
    void getBinaryName() {
        String binaryName = String.class.getName();

        ClassMeta meta = new ClassMeta(binaryName);
        assert meta.getBinaryName().equals(binaryName);
    }

    @Test
    void unsupportedPackage() {
        String binaryName = "HelloWorldClass";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClassMeta(binaryName));
    }
}