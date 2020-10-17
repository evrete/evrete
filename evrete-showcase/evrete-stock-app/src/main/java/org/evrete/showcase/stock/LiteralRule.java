package org.evrete.showcase.stock;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.stock.rule.TimeSlot;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralRule {
    public static final int MAX_FACTS = 4;
    private static final String PREFIX_RULE = "@rule";
    private static final String PREFIX_WHERE = "@where";
    private static final String PREFIX_SELECT = "@select";
    private final String body;
    private final String[] parsedConditions;
    private final Collection<String> factTypeVars;
    private final String name;

    private LiteralRule(String header, String body) throws Exception {
        // Parse header
        String s = header
                .replaceAll("\r\n", "\n")
                .replaceAll("\n\r", "\n");

        String ruleName = null;
        Set<String> conditions = new HashSet<>();
        this.factTypeVars = new HashSet<>();
        for (String line : trimComments(s.split("\n"))) {
            if (line.startsWith(PREFIX_RULE)) {
                ruleName = trimBothEnds(line.substring(PREFIX_RULE.length()));
                if (ruleName.isEmpty()) {
                    ruleName = null;
                } else {
                    ruleName = ruleName
                            .replaceAll("\"", "")
                            .replaceAll("'", "");
                }
            } else if (line.startsWith(PREFIX_WHERE)) {
                String c = trimBothEnds(line.substring(PREFIX_WHERE.length()));
                if (!c.isEmpty()) {
                    conditions.add(c);
                }
            } else if (line.startsWith(PREFIX_SELECT)) {
                String sel = trimBothEnds(line.substring(PREFIX_SELECT.length()));
                String[] parts = sel.split("[\\s,]");
                for (String part : parts) {
                    String tmp = trimBothEnds(part);
                    if (tmp.startsWith("$")) {
                        this.factTypeVars.add(tmp);
                    }
                }
            }
        }

        this.name = ruleName;
        // Sanity checks
        if (this.factTypeVars.isEmpty()) {
            throw new Exception("Invalid rule header format: " + header);
        } else if (factTypeVars.size() > MAX_FACTS) {
            throw new Exception("Too many fact declarations in rule '" + name + "'");
        }

        this.parsedConditions = conditions.toArray(new String[0]);
        this.body = body;
    }

    private static List<String> trimComments(String[] arr) {
        List<String> l = new ArrayList<>();
        for (String s : arr) {
            // Trim spaces on both ends
            s = trimBothEnds(s);
            s = s.replaceAll("(\\s)+$", "");
            // Trim left comment part
            s = s.replaceAll("^/(\\*)+(\\s)*", "");
            // Trim right comment part
            s = s.replaceAll("(\\s)*(\\*)+/$", "");
            // Trim comment continuation
            s = s.replaceAll("^(\\*)+(\\s)*", "");
            if (!s.isEmpty()) {
                l.add(s);
            }
        }
        return l;
    }

    static String trimBothEnds(String s) {
        return s.replaceAll("^(\\s)+", "")
                .replaceAll("(\\s)+$", "");
    }

    public static Knowledge parse(KnowledgeService knowledgeService, String rs, SocketMessenger messenger) throws Exception {
        Knowledge knowledge = knowledgeService.newKnowledge();
        Type<TimeSlot> subjectType = knowledge.getTypeResolver().declare(TimeSlot.class);
        knowledge.getTypeResolver().wrapType(new SlotType(subjectType));

        List<LiteralRule> parsedRules = new LinkedList<>();
        Pattern pattern = Pattern.compile("(?s)/\\*.*?\\*/");
        Matcher m = pattern.matcher(rs);

        // Finding first match
        if (m.find()) {
            int headStart = m.start(), headEnd = m.end();
            String prevHeader = rs.substring(headStart, headEnd);
            int prevHeaderEnd = headEnd;
            while (m.find()) {
                headStart = m.start();
                headEnd = m.end();
                String prevBody = rs.substring(prevHeaderEnd, headStart);
                parsedRules.add(new LiteralRule(prevHeader, prevBody));
                prevHeaderEnd = headEnd;
                prevHeader = rs.substring(headStart, headEnd);
            }
            // Adding the last one
            parsedRules.add(new LiteralRule(prevHeader, rs.substring(prevHeaderEnd)));
        } else {
            throw new Exception("'" + rs + "' is not a valid rule");
        }

        messenger.sendDelayed(new Message("LOG", "Compiling " + parsedRules.size() + " rules..."));
        for (LiteralRule r : parsedRules) {
            RuleBuilder<Knowledge> builder = knowledge.newRule(r.name);
            FactBuilder[] factTypes = r.parsedFactTypes();
            String[] conditions = r.parsedConditions();
            builder
                    .forEach(factTypes)
                    .where(conditions)
                    .setRhs(r.getBody());

            RuleDescriptor descriptor = knowledge.compileRule(builder);
            messenger.sendDelayed(new Message("RULE_COMPILED", descriptor.getName()));
        }
        return knowledge;
    }

    public FactBuilder[] parsedFactTypes() {
        List<FactBuilder> l = new ArrayList<>(factTypeVars.size());

        for (String var : this.factTypeVars) {
            l.add(FactBuilder.fact(var, TimeSlot.class));
        }

        return l.toArray(new FactBuilder[0]);
    }

    public String[] parsedConditions() {
        return parsedConditions;
    }

    public String getBody() {
        return body;
    }

    public static class SlotType extends TypeWrapper<TimeSlot> {
        public SlotType(Type<TimeSlot> delegate) {
            super(delegate);
        }

        @Override
        public TypeField getField(String name) {
            TypeField found = getDelegate().getField(name);
            if (found == null) {
                //Declaring field right in the get method
                found = declareField(name, (ToDoubleFunction<TimeSlot>) subject -> subject.get(name, ConditionSuperClass.UNDEFINED));
            }
            return found;
        }
    }
}
