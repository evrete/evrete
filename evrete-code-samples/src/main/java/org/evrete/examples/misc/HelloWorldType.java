package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.util.TypeResolverWrapper;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class HelloWorldType {
    private static final String HELLO_WORLD_CONST = "Hello World";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge();
        TypeResolver typeResolver = knowledge.getTypeResolver();

        // Declare a new Type named "Hello World"
        Type<String> helloType = typeResolver
                .declare(HELLO_WORLD_CONST, String.class);

        HelloWorldResolver myResolver = new HelloWorldResolver(typeResolver);
        knowledge.wrapTypeResolver(myResolver);
        // Declare a new int field which equals the squared length of the input String
        TypeField lenSquaredField = helloType.declareIntField(
                "lenSquared",
                s -> s.length() * s.length()
        );

        StatefulSession session = knowledge
                .builder()
                .newRule()
                .forEach("$hw", HELLO_WORLD_CONST)
                .where("$hw.lenSquared > 1")
                .execute(context -> {
                    String s = context.get("$hw");
                    int lenSquared = lenSquaredField.readValue(s);
                    System.out.println(s + ", lenSquared = " + lenSquared);
                })
                .build()
                .newStatefulSession();


        session.insertAndFire("a", "bb", "ccc", "dddd");
        /*
            Expected output:
            =====================
            bb, lenSquared = 4
            ccc, lenSquared = 9
            dddd, lenSquared = 16
         */

        session.close();
        service.shutdown();
    }

    static class HelloWorldResolver extends TypeResolverWrapper {
        HelloWorldResolver(TypeResolver delegate) {
            super(delegate);
        }

        @Override
        public <T> Type<T> resolve(Object o) {
            if (o instanceof String) {
                return getType(HELLO_WORLD_CONST);
            } else {
                return super.resolve(o);
            }
        }
    }
}
