package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.api.StatefulSession;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
class PrimeNumbersJava8 {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where(
                        PrimeNumbersJava8::test,
                        "$i1", "$i2", "$i3")
                .execute(); // No RHS

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Change RHS on an active session
            session.getRule("prime numbers").setRhs(PrimeNumbersJava8::rhsMethod);

            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }

            // Execute rules
            session.fire();

            // Print current memory state
            session.forEachFact((h, o) -> System.out.println(o));
        }
        service.shutdown();
    }

    private static void rhsMethod(RhsContext ctx) {
        ctx.deleteFact("$i3");
    }

    private static boolean test(Object[] fieldValues) {
        int i1 = (int) fieldValues[0];
        int i2 = (int) fieldValues[1];
        int i3 = (int) fieldValues[2];
        return i3 == i1 * i2;
    }
}
