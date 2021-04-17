package org.evrete.showcase.chess;

import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.showcase.chess.json.QueenToggleMessage;
import org.evrete.showcase.chess.json.SolutionMessage;
import org.evrete.showcase.chess.types.ChessBoard;
import org.evrete.showcase.chess.types.ChessTask;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ChessSessionWrapper extends AbstractSocketSession {
    private static final int DEFAULT_BOARD_SIZE = 8;
    private final Knowledge knowledge;
    private ChessBoard chessBoard;
    private final AtomicInteger solutions = new AtomicInteger();


    ChessSessionWrapper(Session session) {
        super(session);
        this.chessBoard = new ChessBoard(DEFAULT_BOARD_SIZE);

        try {
            this.knowledge = AppContext.knowledgeService()
                    .newKnowledge("JAVA-CLASS", MainRuleSet.class)
                    .setActivationMode(ActivationMode.CONTINUOUS);
            this.knowledge.set("SOCKET-SESSION", this);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void sendSolution(ChessBoard board) {
        getMessenger().sendUnchecked(new SolutionMessage(solutions.incrementAndGet(), board));
    }

    ChessBoard getChessBoard() {
        return chessBoard;
    }

    ChessBoard reset(int size) {
        this.chessBoard = new ChessBoard(size);
        return this.chessBoard;
    }

    ChessBoard toggle(QueenToggleMessage toggleMessage) {
        return this.chessBoard.toggleQueen(toggleMessage.x, toggleMessage.y);
    }


    void run() {
        closeSession();
        solutions.set(0);
        StatefulSession session = knowledge.createSession();
        super.setKnowledgeSession(session);
        ChessTask initial = new ChessTask(chessBoard);
        getMessenger().sendUnchecked(new Message("INFO", "Session started"));
        session.insertAndFire(initial);
        getMessenger().sendUnchecked(new Message("INFO", "Total solutions: " + solutions.get()));
        getMessenger().sendUnchecked(new Message("STOPPED"));
    }

    @Override
    public boolean closeSession() {
        return super.closeSession();
    }
}
