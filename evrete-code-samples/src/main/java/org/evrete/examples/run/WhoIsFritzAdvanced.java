package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.*;

import java.util.HashMap;

class WhoIsFritzAdvanced {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();
        TypeResolver typeResolver = knowledge.getTypeResolver();

        Type<Subject> subjectType = typeResolver.declare(Subject.class);
        typeResolver.wrapType(new SubjectType(subjectType));

        StatelessSession session = knowledge
                .newRule("rule 1")
                .forEach("$s", Subject.class)
                .where("$s.croaks && $s.eatsFlies && !$s.isFrog")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.set("isFrog");
                    ctx.update($s);
                })
                .newRule("rule 2")
                .forEach("$s", Subject.class)
                .where("$s.isFrog && !$s.green")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.set("green");
                    ctx.update($s);
                })
                .newStatelessSession();

        // Fritz and his known properties
        Subject fritz = new Subject();
        fritz.set("eatsFlies");
        fritz.set("croaks");

        // Insert Fritz and fire all rules
        session.insertAndFire(fritz);

        // Fritz should have been identified as a green frog
        System.out.println(fritz);

        service.shutdown();
    }

    public static class Subject extends HashMap<String, Boolean> {
        void set(String prop) {
            super.put(prop, true);
        }
    }

    public static class SubjectType extends TypeWrapper<Subject> {
        SubjectType(Type<Subject> delegate) {
            super(delegate);
        }

        @Override
        public TypeField getField(String name) {
            TypeField found = super.getField(name);
            if (found == null) {
                // Declaring a new field on the fly
                found = declareBooleanField(
                        name,
                        obj -> Boolean.TRUE.equals(obj.get(name))
                );
            }
            return found;
        }
    }
}
