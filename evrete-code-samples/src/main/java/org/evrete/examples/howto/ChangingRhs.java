package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

public class ChangingRhs {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();

        StatefulSession session = service
                .newKnowledge()
                .newRule("Even numbers")
                .forEach("$i", Integer.class)
                .where("$i % 2 == 0")
                .execute(ctx -> {
                    int evenNumber = ctx.get("$i");
                    System.out.printf("\t%d%n", evenNumber);
                })
                .newStatefulSession();

        // 1. Initial test
        System.out.println("1. Testing the rule as-is:");
        session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

        // 2. Replacing the RHS
        System.out.println("2. Replacing the action:");
        session.getRule("Even numbers").setRhs(ctx -> {
            int evenNumber = ctx.get("$i");
            System.out.println("\t'" + evenNumber + "'");
        });
        session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

        // 3. Chaining actions
        System.out.println("3. Chaining rule's actions:");
        session.getRule("Even numbers").chainRhs(ctx -> {
            int evenNumber = ctx.get("$i");
            System.out.println("\tChained action on '" + evenNumber + "'");
        });
        session.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

        // Closing resources
        session.close();
        service.shutdown();
    }

}
