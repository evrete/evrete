package org.evrete.showcase.chess.json;

import org.evrete.showcase.chess.types.ChessBoard;
import org.evrete.showcase.shared.JsonMessage;

public class BoardMessage extends JsonMessage {
    public ChessBoard board;

    public BoardMessage(ChessBoard board) {
        super("BOARD");
        this.board = board;
    }
}
