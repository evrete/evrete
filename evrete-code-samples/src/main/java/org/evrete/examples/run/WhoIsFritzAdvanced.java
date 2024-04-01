package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.util.TypeWrapper;

import java.util.HashMap;

public class WhoIsFritzAdvanced {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();
        TypeResolver typeResolver = knowledge.getTypeResolver();

        Type<Subject> subjectType = typeResolver.declare(Subject.class);
        typeResolver.wrapType(new SubjectType(subjectType));

        StatelessSession session = knowledge
                .builder()
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.croaks && $s.eatsFlies && !$s.isFrog")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.set("isFrog");
                    ctx.update($s);
                })
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.isFrog && !$s.green")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.set("green");
                    ctx.update($s);
                })
                .build()
                .newStatelessSession();

        // Init subject and its known properties
        Subject fritz = new Subject();
        fritz.set("eatsFlies");
        fritz.set("croaks");

        // Insert Fritz and fire all rules
        session.insertAndFire(fritz);
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

        @NonNull
        @Override
        public TypeField getField(@NonNull String name) {
            // Declaring a new field on the fly
            return declareBooleanField(
                    name,
                    obj -> Boolean.TRUE.equals(obj.get(name))
            );
        }
    }
}
