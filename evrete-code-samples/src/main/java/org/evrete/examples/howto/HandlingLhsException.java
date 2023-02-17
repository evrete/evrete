package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.*;

import static java.lang.System.out;

public class HandlingLhsException {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        RuleBuilder<Knowledge> builder = knowledge.newRule("Test rule");
        LhsBuilder<Knowledge> lhsBuilder = builder.forEach("$i", Integer.class);

        // Without exception handling, the condition below will throw an ArithmeticException
        EvaluatorHandle handle = builder.createCondition("$i / 0 == 1");
        Evaluator failingEvaluator = knowledge.getEvaluator(handle);

        // We want our rule to print matching numbers
        lhsBuilder.execute(ctx -> {
                    int i = ctx.get("$i");
                    out.println(i);
                }
        );

        // Replacing the condition
        knowledge.replaceEvaluator(handle, new Evaluator() {
            @Override
            public FieldReference[] descriptor() {
                return failingEvaluator.descriptor();
            }

            @Override
            public boolean test(IntToValue t) {
                try {
                    return failingEvaluator.test(t);
                } catch (IllegalStateException e) {
                    assert e.getCause() instanceof ArithmeticException;
                    return true;
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
