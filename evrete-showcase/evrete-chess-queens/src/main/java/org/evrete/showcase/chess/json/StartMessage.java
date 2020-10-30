package org.evrete.showcase.chess.json;

import org.evrete.showcase.chess.types.Cell;
import org.evrete.showcase.shared.JsonMessage;

import java.util.Arrays;

public class StartMessage extends JsonMessage {
    public Cell[][] cells;

    public StartMessage() {
        super("BOARD");
    }

    @Override
    public String toString() {
        return "StartMessage{" +
                "cells=" + Arrays.deepToString(cells) +
                '}';
    }
}
