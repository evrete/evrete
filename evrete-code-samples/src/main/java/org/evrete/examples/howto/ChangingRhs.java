package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;

import java.util.Objects;

import static java.lang.System.out;

public class ChangingRhs {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule("Even numbers")
                .forEach("$i", Integer.class)
                .where("$i % 2 == 0")
                .execute(ctx -> {
                    int $i = ctx.get("$i");
                    out.printf("\t%d%n", $i);
                })
                .build();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            RuntimeRule rule = Objects.requireNonNull(session.getRule("Even numbers"));

            // 1. Initial test
            out.println("1. Testing the rule as-is:");
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 2. Replacing the RHS
            out.println("2. Replaced action test:");
            rule.setRhs(ctx -> {
                int $i = ctx.get("$i");
                out.printf("\t'%d'%n", $i);
            });
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 3. Chaining actions
            out.println("3. Chained action test:");
            rule.chainRhs(ctx -> {
                int $i = ctx.get("$i");
                out.printf("\tchained action on [%d]%n", $i);
            });
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        // Closing resources
        service.shutdown();
    }
}
