package org.evrete.showcase.chess;

import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.api.StatefulSession;
import org.evrete.showcase.chess.json.QueenToggleMessage;
import org.evrete.showcase.chess.types.ChessBoard;
import org.evrete.showcase.chess.types.ChessTask;
import org.evrete.showcase.chess.types.WorkingQueue;
import org.evrete.showcase.shared.AbstractSocketSession;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;

import javax.websocket.Session;
import java.util.concurrent.atomic.AtomicBoolean;

class ChessSessionWrapper extends AbstractSocketSession {
    private static final int DEFAULT_BOARD_SIZE = 8;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Knowledge knowledge;
    private ChessBoard chessBoard;

    public ChessSessionWrapper(Session session) {
        super(session);
        this.chessBoard = new ChessBoard(DEFAULT_BOARD_SIZE);


        this.knowledge = AppContext.knowledgeService().newKnowledge();
        knowledge.newRule("Inject sub-task")
                .forEach(
                        "$queue", WorkingQueue.class
                )
                .where("$queue.size > 0")
                //.where("$queue.size < 50")
                .execute(this::rule1rhs)

                .newRule("Report completed")
                .forEach(
                        "$task", ChessTask.class,
                        "$queue", WorkingQueue.class
                )
                .where("$task.completed == true")
                .execute(this::rule2rhs)

                .newRule("Delete failed")
                .forEach(
                        "$task", ChessTask.class,
                        "$queue", WorkingQueue.class
                )
                .where("$task.failed == true")
                .execute(this::rule3rhs)

                .newRule("Handle task by the queue")
                .forEach(
                        "$task", ChessTask.class,
                        "$queue", WorkingQueue.class
                )
                .where("$task.completed == false")
                .where("$task.failed == false")
                //.where("$queue.size < 20")
                .execute(this::rule4rhs)

                .newRule("Update queue")
                .forEach(
                        "$queue", WorkingQueue.class
                )
                .where("$queue.size > 0")
                .execute(this::rule5rhs)
        ;
    }

    private void rule1rhs(RhsContext ctx) {
        WorkingQueue $queue = ctx.get("$queue");
        ChessTask subTask = $queue.take();
        ctx.insert(subTask);
    }

    private void rule2rhs(RhsContext ctx) {
        ChessTask $task = ctx.get("$task");
        getMessenger().sendUnchecked(new Message("FOUND"));
        ctx.delete($task);
    }

    private void rule3rhs(RhsContext ctx) {
        ChessTask $task = ctx.get("$task");
        ctx.delete($task);
    }

    private void rule4rhs(RhsContext ctx) {
        WorkingQueue $queue = ctx.get("$queue");
        ChessTask $task = ctx.get("$task");
        $queue.process($task);

        ctx.delete($task);
        ctx.update($queue);
    }

    private void rule5rhs(RhsContext ctx) {
        WorkingQueue $queue = ctx.get("$queue");
        ctx.update($queue);
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
        super.setKnowledgeSession(knowledge.createSession());
        new SessionThread().start();
    }

    @Override
    public boolean closeSession() {
        return super.closeSession();
    }

    public class SessionThread extends Thread {

        SessionThread() {
        }

        @Override
        public void run() {
            System.out.println("$$$$$ - start");
            final StatefulSession session = getKnowledgeSession();
            final SocketMessenger messenger = getMessenger();
            ChessTask initial = new ChessTask(chessBoard);
            session.insertAndFire(
                    new WorkingQueue(initial)
            );
            try {
                running.set(true);
                session.fire();
            } catch (Exception e) {
                messenger.send(e);
            } finally {
                running.set(false);
                messenger.sendUnchecked(new Message("STOPPED"));
                System.out.println("$$$$$ - end");
            }
        }
    }


}
