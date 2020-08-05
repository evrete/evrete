package org.evrete.api;

@FunctionalInterface
public interface IntToValueRow {

    ValueRow apply(int value);
}
