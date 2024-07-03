package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.EvaluatorHandle;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.ValuesPredicate;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;

import java.util.concurrent.CompletableFuture;

import static java.lang.System.out;

public class HandlingLhsException {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        RuleBuilder<Knowledge> builder = knowledge
                .builder()
                .newRule("Test rule");
        LhsBuilder<Knowledge> lhsBuilder = builder.forEach("$i", Integer.class);

        // Without exception handling, the condition below will throw an ArithmeticException
        CompletableFuture<EvaluatorHandle> futureHandle = builder.getConditionManager().addCondition("$i / 0 == 1");

        // We want our rule to print matching numbers
        lhsBuilder
                .execute(ctx -> {
                            int i = ctx.get("$i");
                            out.println(i);
                        }
                )
                .build();

        EvaluatorHandle handle = futureHandle.join();
        ValuesPredicate failingEvaluator = knowledge.getEvaluatorsContext().getPredicate(handle);

        // Replacing the condition
        knowledge.getEvaluatorsContext().replacePredicate(handle, t -> {
            try {
                return failingEvaluator.test(t);
            } catch (Throwable e) {
                if(e.getCause() instanceof ArithmeticException) {
                    return true;
                } else {
                    throw new IllegalStateException("Unexpected exception", e);
                }
            }
        });

        // Testing the rule
        try (StatefulSession s = knowledge.newStatefulSession()) {
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        service.shutdown();
    }
}
