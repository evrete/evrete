package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import static java.lang.System.out;

public class ChangingConditions {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // 1. Creating a rule and getting its condition handle
        RuleBuilder<Knowledge> builder = knowledge.newRule("Even numbers");
        LhsBuilder<Knowledge> lhsBuilder = builder.forEach("$i", Integer.class);
        EvaluatorHandle handle = lhsBuilder.addWhere("$i % 2 == 0");
        lhsBuilder.execute(ctx -> {
            int $i = ctx.get("$i");
            out.printf("\t%d%n", $i);
        });
        out.println("1. Rule created.");

        try (StatefulSession s = knowledge.newStatefulSession()) {
            RuntimeRule rule = s.getRule("Even numbers");
            // 2. Initial test
            out.println("\n2. Testing the rule as-is:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 3. Building a new expression and replacing the old one
            Evaluator newCondition = rule.buildExpression("$i % 2 == 1");
            s.replaceEvaluator(handle, newCondition);
            out.println("\n3. Testing new condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 4. Yet another condition, as a Predicate this time
            s.replaceEvaluator(handle, t -> {
                int $i = t.get(0);
                return $i % 3 == 0;
            });
            out.println("\n4. Testing another condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        try (StatefulSession s = knowledge.newStatefulSession()) {
            // 5. New session shouldn't be affected by the changes
            out.println("\n5. Testing a new session:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        service.shutdown();
    }
}
