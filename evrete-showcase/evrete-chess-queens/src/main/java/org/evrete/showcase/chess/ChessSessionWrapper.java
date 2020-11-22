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
import org.evrete.showcase.shared.Utils;

import javax.websocket.Session;
import java.util.concurrent.atomic.AtomicInteger;

class ChessSessionWrapper extends AbstractSocketSession {
    private static final int DEFAULT_BOARD_SIZE = 8;
    private final Knowledge knowledge;
    private ChessBoard chessBoard;
    private final AtomicInteger solutions = new AtomicInteger();


    public ChessSessionWrapper(Session session) {
        super(session);
        this.chessBoard = new ChessBoard(DEFAULT_BOARD_SIZE);

        this.knowledge = AppContext.knowledgeService()
                .newKnowledge()
                .newRule("Report completed")
                .forEach("$task", ChessTask.class)
                .where("$task.completed == true")
                .execute(ctx -> {
                    ChessTask $task = ctx.get("$task");
                    getMessenger().sendUnchecked(new SolutionMessage(solutions.incrementAndGet(), $task.board));
                    ctx.delete($task);
                    Utils.delay(20);
                })

                .newRule("Remove failed")
                .forEach("$task", ChessTask.class)
                .where("$task.failed == true")
                .execute(ctx -> {
                    ChessTask $task = ctx.get("$task");
                    ctx.delete($task);
                })

                .newRule("Spawn subtask")
                .forEach("$task", ChessTask.class)
                .where("$task.failed == false")
                .where("$task.completed == false")
                .execute(ctx -> {
                    ChessTask $task = ctx.get("$task");
                    ctx.insert($task.nextTask());
                    ctx.update($task);
                });
        //TODO move to DEFAULT
        this.knowledge.setActivationMode(ActivationMode.CONTINUOUS);
    }


    public ChessBoard getChessBoard() {
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
