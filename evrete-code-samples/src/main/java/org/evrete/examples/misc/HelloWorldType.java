package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class HelloWorldType {
    private static final String HELLO_WORLD_TYPE = "Hello World";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service.newKnowledge()
                .configureTypes(typeResolver -> {
                    // Declare a new Type named "Hello World"
                    Type<String> helloType = typeResolver
                            .declare(HELLO_WORLD_TYPE, String.class);

                    // Declare a new int field which equals the squared length of the input String
                    helloType.declareIntField(
                            "lenSquared",
                            s -> s.length() * s.length()
                    );
                });

        // Obtain the created field (we'll use it in the action)
        final TypeField lenSquaredField = knowledge
                .getTypeResolver()
                .getType(HELLO_WORLD_TYPE)
                .getField("lenSquared");


        StatefulSession session = knowledge
                .builder()
                .newRule()
                .forEach("$hw", HELLO_WORLD_TYPE)
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
}
