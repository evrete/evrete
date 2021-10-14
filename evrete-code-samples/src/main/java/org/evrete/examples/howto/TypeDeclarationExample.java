package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.TypeField;

class TypeDeclarationExample {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // Field declaration
        TypeField factorialField = knowledge
                .getTypeResolver()
                .getOrDeclare(Integer.class)
                .declareLongField(
                        "factorial",
                        i -> {
                            long f = 1L;
                            for (int t = 1; t <= i; t++) f *= t;
                            return f;
                        }
                );

        // New field in a rule
        StatelessSession session = knowledge
                .newRule()
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class)
                .where("$i1.factorial > $i2.factorial")
                .execute(ctx -> {
                    Integer i1 = ctx.get("$i1");
                    Integer i2 = ctx.get("$i2");
                    long factorial1 = factorialField.readValue(i1);
                    long factorial2 = factorialField.readValue(i2);
                    System.out.printf("i1: %d (%d)\t\ti2: %d (%d)%n",
                            i1,
                            factorial1,
                            i2,
                            factorial2
                    );
                })
                .newStatelessSession();

        // Testing the rule
        session.insertAndFire(4, 5, 6);

        service.shutdown();
    }
}