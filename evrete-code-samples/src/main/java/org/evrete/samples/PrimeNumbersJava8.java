package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.RhsContext;
import org.evrete.api.StatefulSession;

public class PrimeNumbersJava8 {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where(
                        PrimeNumbersJava8::test,
                        "$i1.intValue",
                        "$i2.intValue",
                        "$i3.intValue")
                .execute() // No RHS
                .createSession();

        // Change RHS on an active session
        session.getRule("prime numbers").setRhs(PrimeNumbersJava8::rhsMethod);


        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }

        // Execute rules
        session.fire();

        // Print current memory state
        session.forEachMemoryObject(System.out::println);

        // Closing resources
        session.close();
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
