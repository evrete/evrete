package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.Type;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
class TypeDeclarationImplicit {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service
                .newKnowledge()
                .configureTypes(typeResolver -> {
                    Type<Long> type = typeResolver
                            .getOrDeclare(Long.class);
                    type.declareDoubleField("asDouble", value -> value * 1.0);
                })
                .builder()
                .newRule()
                .forEach("$l", Long.class) // Implicit type declaration
                .where("$l.asDouble > 0")
                .execute(context -> {
                    Long fact = context.get("$l");
                    System.out.println(fact);
                })
                .build();

        StatelessSession session = knowledge.newStatelessSession();

        // Implicit insert
        session
                .insert(-1234L, 1234L)
                .fire();

        service.shutdown();
    }
}
