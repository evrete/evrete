package org.evrete.runtime.memory;

import org.evrete.api.Action;

import java.util.Collection;
import java.util.EnumMap;

public class ActionBuffer<T> {
    private final EnumMap<Action, Collection<T>> data = new EnumMap<>(Action.class);
}
