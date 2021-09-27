package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class WhoIsFritzAnnotated {
    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge(
                        "JAVA-CLASS",
                        WhoIsFritzAnnotated.RuleSet.class);

        // Init subject and it's known properties
        Subject fritz = new Subject();
        fritz.croaks = true;
        fritz.eatsFlies = true;

        // Insert Fritz and fire all rules
        knowledge.newStatelessSession().insertAndFire(fritz);

        // Fritz should have been identified as a green frog
        System.out.println("Is Fritz a frog?\t" + fritz.isFrog);
        System.out.println("Is Fritz green? \t" + fritz.green);

        service.shutdown();
    }

    public static class Subject {
        public boolean croaks;
        public boolean eatsFlies;
        public boolean isFrog;
        public boolean green;
    }

    public static class RuleSet {
        @Rule
        @Where({"$s.isFrog", "!$s.green"})
        public void rule1(RhsContext ctx, @Fact("$s") Subject $s) {
            $s.green = true;
            ctx.update($s);
        }

        @Rule
        @Where({"$s.croaks", "$s.eatsFlies", "!$s.isFrog"})
        public void rule2(RhsContext ctx, @Fact("$s") Subject $s) {
            $s.isFrog = true;
            ctx.update($s);
        }
    }
}
