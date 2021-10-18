package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.*;

public class ChangingConditions {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge();
        RuleBuilder<Knowledge> builder = knowledge
                .newRule("Even numbers");


        LhsBuilder<Knowledge> lhsBuilder = builder.forEach("$i", Integer.class);
        EvaluatorHandle handle = lhsBuilder.addWhere("$i % 2 == 0");
        lhsBuilder.execute(ctx -> {
            int evenNumber = ctx.get("$i");
            System.out.printf("\t%d%n", evenNumber);
        });

        try(StatefulSession s = knowledge.newStatefulSession()) {
            // 1. Initial test
            System.out.println("1. Testing the rule as-is:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);

            RuntimeRule rule = s.getRule("Even numbers");

            Evaluator newCondition =  rule.buildExpression("$i % 2 == 1");

            s.replaceEvaluator(handle, newCondition);
            //s.replaceEvaluator();

            rule.setName("Odd numbers");

            System.out.println("2. Testing new condition:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }

        try(StatefulSession s = knowledge.newStatefulSession()) {
            // 1. Initial test
            System.out.println("3. Testing another session:");
            s.insertAndFire(0, 1, 2, 3, 4, 5, 6, 7);
        }


        service.shutdown();
    }

}
