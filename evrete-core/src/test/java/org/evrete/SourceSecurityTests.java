package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.StatefulSession;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilePermission;

class SourceSecurityTests {
    private Knowledge knowledge;

    @BeforeEach
    void init() {
        KnowledgeService service = new KnowledgeService();
        knowledge = service.newKnowledge();
        knowledge.addImport(RuleScope.BOTH, File.class);
    }

    @Test
    void alphaConditionFail() {
        assert System.getSecurityManager() != null;
        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    knowledge.newRule()
                            .forEach(
                                    "$a", TypeA.class
                            )
                            .where("new File($a.id).exists()")
                            .execute();

                    try(StatefulSession session = knowledge.newStatefulSession()){
                        TypeA a = new TypeA("id");
                        session.insert(a);
                        session.fire();
                    }
                }
        );
    }

    @Test
    void alphaConditionPass() {
        assert System.getSecurityManager() != null;
        knowledge.getService().getSecurity().addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class
                )
                .where("new File($a.id).exists()")
                .execute();

        try(StatefulSession session = knowledge.newStatefulSession()){
            TypeA a = new TypeA("id");
            session.insert(a);
            session.fire();
        }
    }

    @Test
    void betaConditionFail1() {
        assert System.getSecurityManager() != null;
        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    knowledge.newRule()
                            .forEach(
                                    "$a", TypeA.class,
                                    "$b", TypeB.class
                            )
                            .where("new File($a.id).exists() && new File($b.id).exists()")
                            .execute();

                    try(StatefulSession session = knowledge.newStatefulSession()){
                        TypeA a = new TypeA("id");
                        TypeB b = new TypeB("id");
                        session.insert(a, b);
                        session.fire();
                    }
                }
        );
    }

    @Test
    void betaConditionFail2() {
        assert System.getSecurityManager() != null;
        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    knowledge.getService().getSecurity().addPermission(RuleScope.RHS, new FilePermission("<<ALL FILES>>", "read"));
                    knowledge.newRule()
                            .forEach(
                                    "$a", TypeA.class,
                                    "$b", TypeB.class
                            )
                            .where("new File($a.id).exists() && new File($b.id).exists()")
                            .execute();

                    try(StatefulSession session = knowledge.newStatefulSession()){
                        TypeA a = new TypeA("id");
                        TypeB b = new TypeB("id");
                        session.insert(a, b);
                        session.fire();
                    }
                }
        );
    }

    @Test
    void betaConditionPass1() {
        assert System.getSecurityManager() != null;
        knowledge.getService().getSecurity().addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("new File($a.id).exists() && new File($b.id).exists()")
                .execute();

        try(StatefulSession session = knowledge.newStatefulSession()){
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }

    @Test
    void betaConditionPass2() {
        assert System.getSecurityManager() != null;
        knowledge.getService().getSecurity().addPermission(RuleScope.LHS, new FilePermission("<<ALL FILES>>", "read"));
        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("new File($a.id).exists() && new File($b.id).exists()")
                .execute();

        try(StatefulSession session = knowledge.newStatefulSession()){
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }

    @Test
    void betaConditionPass3() {
        assert System.getSecurityManager() != null;
        // No illegal access and no permissions
        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.id == $b.id")
                .execute(ctx -> {
                });

        try(StatefulSession session = knowledge.newStatefulSession()) {
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }


    @Test
    void rhsPass1() {
        assert System.getSecurityManager() != null;
        knowledge.getService().getSecurity().addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        knowledge.newRule("RHS test")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.id == $b.id")
                .execute("new File(\"\").exists();");

        try(StatefulSession session = knowledge.newStatefulSession()){
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }

    @Test
    void rhsPass2() {
        assert System.getSecurityManager() != null;
        knowledge.getService().getSecurity().addPermission(RuleScope.RHS, new FilePermission("<<ALL FILES>>", "read"));
        knowledge.newRule("RHS test")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.id == $b.id")
                .execute("new File(\"\").exists();");

        try(StatefulSession session = knowledge.newStatefulSession()){
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }

    @Test
    void rhsPass3() {
        assert System.getSecurityManager() != null;
        // No illegal access and no permissions
        knowledge.newRule("RHS test")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.id == $b.id")
                .execute("");

        try(StatefulSession session = knowledge.newStatefulSession()) {
            TypeA a = new TypeA("id");
            TypeB b = new TypeB("id");
            session.insert(a, b);
            session.fire();
        }
    }

    @Test
    void rhsFail1() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    knowledge.getService().getSecurity().addPermission(RuleScope.LHS, new FilePermission("<<ALL FILES>>", "read"));
                    knowledge.newRule("RHS test")
                            .forEach(
                                    "$a", TypeA.class,
                                    "$b", TypeB.class
                            )
                            .where("$a.id == $b.id")
                            .execute("new File(\"\").exists();");

                    try(StatefulSession session = knowledge.newStatefulSession()){
                        TypeA a = new TypeA("id");
                        TypeB b = new TypeB("id");
                        session.insert(a, b);
                        session.fire();
                    }
                }
        );
    }

    @Test
    void rhsFail2() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    knowledge.newRule("RHS test")
                            .forEach(
                                    "$a", TypeA.class,
                                    "$b", TypeB.class
                            )
                            .where("$a.id == $b.id")
                            .execute("new File(\"\").exists();");

                    try(StatefulSession session = knowledge.newStatefulSession()){
                        TypeA a = new TypeA("id");
                        TypeB b = new TypeB("id");
                        session.insert(a, b);
                        session.fire();
                    }
                }
        );
    }


}
