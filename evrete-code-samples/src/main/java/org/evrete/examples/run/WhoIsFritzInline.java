package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;

public class WhoIsFritzInline {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.isFrog").where("!$s.green")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.green = true;
                    ctx.update($s);
                })
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.croaks").where("$s.eatsFlies").where("!$s.isFrog")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.isFrog = true;
                    ctx.update($s);
                })
                .build();

        // Init subject and its known properties
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
}
