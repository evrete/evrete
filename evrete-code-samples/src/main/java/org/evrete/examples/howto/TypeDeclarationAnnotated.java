package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.StatelessSession;
import org.evrete.dsl.annotation.*;

class TypeDeclarationAnnotated {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        StatelessSession session = service
            .newKnowledge(
                "JAVA-CLASS",
                FactorialRuleset.class
            )
            .newStatelessSession();

        // Testing the rule
        session.insertAndFire(4, 5, 6);
        service.shutdown();
    }

    @RuleSet
    public static class FactorialRuleset {

        @Rule
        @Where("$i1.factorial > $i2.factorial")
        public void rule1(@Fact("$i1") Integer i1, @Fact("$i2") Integer i2) {
            long factorial1 = factorial(i1);
            long factorial2 = factorial(i2);
            System.out.printf("i1: %d (%d)\t\ti2: %d (%d)%n",
                i1,
                factorial1,
                i2,
                factorial2
            );
        }

        @FieldDeclaration
        public long factorial(Integer i) {
            long f = 1L;
            for (int t = 1; t <= i; t++) f *= t;
            return f;
        }
    }
}