package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.runtime.compiler.CompilationException;

import static java.lang.System.out;

public class ChangingConditions {

    public static void main(String[] args) throws CompilationException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // 1. Creating a rule and getting its condition handle
        RuleBuilder<Knowledge> builder = knowledge
                .builder()
                .newRule("Even numbers");
        LhsBuilder<Knowledge> lhsBuilder = builder.forEach("$i", Integer.class);
        EvaluatorHandle handle = builder.createCondition("$i % 2 == 0");

        // 2. Using the handle in the "where" instruction
        lhsBuilder.where(handle);
        lhsBuilder.execute(ctx -> {
            int $i = ctx.get("$i");
            out.printf("\t%d%n", $i);
                })
                .build();
        out.println("1. Rule created.");

        try (StatefulSession s = knowledge.newStatefulSession()) {
            RuntimeRule rule = s.getRule("Even numbers");
            // 3. Initial test
            out.println("\n2. Testing the rule as-is:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 4. Building a new expression and replacing the old one
            Evaluator newCondition = rule.buildExpression("$i % 2 == 1");
            s.replaceEvaluator(handle, newCondition);
            out.println("\n3. Testing new condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 5. Yet another condition, as a Predicate this time
            s.replaceEvaluator(handle, t -> {
                int $i = t.get(0);
                return $i % 3 == 0;
            });
            out.println("\n4. Testing another condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        try (StatefulSession s = knowledge.newStatefulSession()) {
            // 6. New session shouldn't be affected by the changes
            out.println("\n5. Testing a new session:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        service.shutdown();
    }
}
