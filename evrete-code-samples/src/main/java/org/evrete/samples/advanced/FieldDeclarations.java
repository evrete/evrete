package org.evrete.samples.advanced;

import org.evrete.KnowledgeService;
import org.evrete.api.*;

class FieldDeclarations {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge();
        TypeResolver typeResolver = knowledge.getTypeResolver();

        Type<Integer> type = typeResolver.getOrDeclare(Integer.class);

        // Despite being defined, this field will never be evaluated,
        // because it's not a part of any condition
        type.declareField(
                "self",
                Integer.class, o -> o);

        type.declareField("factorial", FieldDeclarations::computeFactorial);

        StatefulSession session = knowledge
                .newRule()
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class)
                .where("$i1.factorial > $i2.factorial")
                .execute(context -> {
                    RuntimeFact fact1 = context.getFact("$i1");
                    RuntimeFact fact2 = context.getFact("$i2");
                    int factObject1 = fact1.getDelegate();
                    int factObject2 = fact2.getDelegate();
                    long factorial1 = fact1.getValue(0);
                    long factorial2 = fact2.getValue(0);

                    System.out.printf("i1: %d (%d)\t\ti2: %d (%d)\n", factObject1, factorial1, factObject2, factorial2);
                })
                .createSession();


        session.insertAndFire(4, 5, 6);
        /*
            Expected output:
            =====================
            i1: 5 (120)		i2: 4 (24)
            i1: 6 (720)		i2: 4 (24)
            i1: 6 (720)		i2: 5 (120)
         */

        session.close();
        service.shutdown();
    }

    private static long computeFactorial(int value) {
        long result = 1L;
        for (int i = 1; i <= value; i++) {
            result *= i;
        }
        return result;
    }
}