package org.evrete.showcase.chess.json;

import org.evrete.showcase.shared.JsonMessage;

public class QueenToggleMessage extends JsonMessage {
    public int x;
    public int y;

    public QueenToggleMessage() {
        super("TOGGLE");
    }

}
