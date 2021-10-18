package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import static java.lang.System.out;

public class ChangingRhs {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("Even numbers")
                .forEach("$i", Integer.class)
                .where("$i % 2 == 0")
                .execute(ctx -> {
                    int $i = ctx.get("$i");
                    out.printf("\t%d%n", $i);
                });

        try(StatefulSession session = knowledge.newStatefulSession()) {
            RuntimeRule rule = session.getRule("Even numbers");

            // 1. Initial test
            out.println("1. Testing the rule as-is:");
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 2. Replacing the RHS
            out.println("2. Replacing the action:");
            rule.setRhs(ctx -> {
                int $i = ctx.get("$i");
                out.printf("\t'%d'%n", $i);
            });
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 3. Chaining actions
            System.out.println("3. Chaining the rule's action:");
            rule.chainRhs(ctx -> {
                int $i = ctx.get("$i");
                out.printf("\tChained action on [%d]%n", $i);
            });
            session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        // Closing resources
        service.shutdown();
    }
}
