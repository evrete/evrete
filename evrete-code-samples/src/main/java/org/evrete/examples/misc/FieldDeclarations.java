package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.TypeField;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
class FieldDeclarations {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge()
                .configureTypes(typeResolver -> typeResolver
                        .getOrDeclare(Integer.class)
                        .declareLongField(
                                "factorial",
                                FieldDeclarations::computeFactorial));

        // Get the field reference (we'll test it in the rule actions)
        TypeField factorialField = knowledge
                .getTypeResolver()
                .getOrDeclare(Integer.class)
                .getField("factorial");

        StatefulSession session = knowledge
                .builder()
                .newRule()
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class)
                .where("$i1.factorial > $i2.factorial")
                .execute(context -> {
                    Integer i1 = context.get("$i1");
                    Integer i2 = context.get("$i2");
                    long factorial1 = factorialField.readValue(i1);
                    long factorial2 = factorialField.readValue(i2);
                    System.out.printf("i1: %d (%d)\t\ti2: %d (%d)\n", i1, factorial1, i2, factorial2);

                })
                .build()
                .newStatefulSession();


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
