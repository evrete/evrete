package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

import java.io.IOException;

@SuppressWarnings({"unused", "UseOfSystemOutOrSystemErr"})
public class PrimeNumbersLiteralSource {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        String source = "package com.test;\n" +
                "import org.evrete.dsl.annotation.*;\n" +
                "import org.evrete.api.RhsContext;\n" +
                "\n" +
                "public class PrimeNumbersKnowledge {\n" +
                "\n" +
                "    @Rule\n" +
                "    @Where(\"$i1 * $i2 == $i3\")\n" +
                "    public static void rule(RhsContext ctx, int $i1, int $i2, int $i3) {\n" +
                "        ctx.deleteFact(\"$i3\");\n" +
                "    }\n" +
                "}\n";
        StatefulSession session = service
                .newKnowledge()
                .builder()
                .importRules("JAVA-SOURCE", source)
                .build()
                .newStatefulSession();

        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }
        // Execute rules
        session.fire();
        // Printout current memory state
        session.forEachFact((handle, o) -> System.out.print(o + " "));
        session.close();
    }
}
