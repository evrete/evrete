package org.evrete.showcase.chess;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import org.evrete.showcase.chess.types.ChessTask;
import org.evrete.showcase.shared.Utils;

public class MainRuleSet {

    @Rule("Report completed")
    @Where("$task.completed == true")
    public static void rule1(RhsContext ctx, @Fact("$task") ChessTask $task) {
        ChessSessionWrapper session = ctx.getRuntime().get("SOCKET-SESSION");
        session.sendSolution($task.board);
        ctx.delete($task);
        Utils.delay(20);
    }

    @Rule("Remove failed")
    @Where("$task.failed == true")
    public static void rule2(RhsContext ctx, @Fact("$task") ChessTask $task) {
        ctx.delete($task);
    }

    @Rule("Spawn subtask")
    @Where({"$task.failed == false", "$task.completed == false"})
    public static void rule3(RhsContext ctx, @Fact("$task") ChessTask $task) {
        ctx.insert($task.nextTask());
        ctx.update($task);
    }
}
