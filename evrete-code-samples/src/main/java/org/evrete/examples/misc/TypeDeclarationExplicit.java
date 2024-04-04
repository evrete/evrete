package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.Type;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
class TypeDeclarationExplicit {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service
                .newKnowledge()
                .configureTypes(typeResolver -> {
                    Type<Long> type = typeResolver
                            .declare("Custom Type", Long.class);
                    type.declareDoubleField("asDouble", value -> value * 1.0);
                })
                .builder()
                .newRule()
                .forEach("$l", "Custom Type") // Explicit declaration
                .where("$l.asDouble > 0")
                .execute(context -> {
                    Long fact = context.get("$l");
                    System.out.println(fact);
                })
                .build();

        StatelessSession session = knowledge.newStatelessSession();

        // Explicit insert
        session
                .insertAs("Custom Type", -1234L, 1234L)
                .fire();

        service.shutdown();
    }
}
