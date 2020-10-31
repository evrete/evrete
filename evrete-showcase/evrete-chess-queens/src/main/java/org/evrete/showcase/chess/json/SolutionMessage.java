package org.evrete.showcase.chess.json;

import org.evrete.showcase.chess.types.ChessBoard;
import org.evrete.showcase.shared.JsonMessage;

public class SolutionMessage extends JsonMessage {
    public ChessBoard board;
    public int id;

    public SolutionMessage(int id, ChessBoard board) {
        super("SOLUTION");
        this.board = board;
        this.id = id;
    }
}
