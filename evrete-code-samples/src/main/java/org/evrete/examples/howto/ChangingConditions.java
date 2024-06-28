package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.ConditionManager;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.util.CompilationException;

import java.util.concurrent.CompletableFuture;

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

        ConditionManager conditionManager = builder.getConditionManager();
        CompletableFuture<EvaluatorHandle> futureHandle = conditionManager.addCondition("$i % 2 == 0");

        // 2. Using the handle in the "where" instruction
        lhsBuilder.execute(ctx -> {
                    int $i = ctx.get("$i");
                    out.printf("\t%d%n", $i);
                })
                .build();
        out.println("1. Rule created.");

        // When the build process is complete and error-free, we can the condition itself
        EvaluatorHandle handle = futureHandle.join();


        try (StatefulSession s = knowledge.newStatefulSession()) {
            // 3. Initial test
            out.println("\n2. Testing the rule as-is:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 4. Building a new expression and replacing the old one
            s.getEvaluatorsContext().replacePredicate(handle, t -> t.get(0, int.class) % 2 == 1);
            out.println("\n3. Testing new condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            // 5. Yet another condition, as a Predicate this time
            s.getEvaluatorsContext().replacePredicate(handle, t -> t.get(0, int.class) % 3 == 0);
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
