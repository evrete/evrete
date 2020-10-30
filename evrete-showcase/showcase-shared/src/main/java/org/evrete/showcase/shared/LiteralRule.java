package org.evrete.showcase.shared;

import org.evrete.api.FactBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralRule {
    private static final String PREFIX_RULE = "@rule";
    private static final String PREFIX_WHERE = "@where";
    private static final String PREFIX_SELECT = "@select";
    private final String body;
    private final String[] parsedConditions;
    private final Collection<String> factTypeVars;
    private final String name;

    private LiteralRule(String header, String body) {
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

        this.parsedConditions = conditions.toArray(new String[0]);
        this.body = body;
    }

    private static String trimBothEnds(String s) {
        return s.replaceAll("^(\\s)+", "")
                .replaceAll("(\\s)+$", "");
    }

    public static List<LiteralRule> parse(String rs) throws Exception {

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

        return parsedRules;
    }

    public String getName() {
        return name;
    }


    public Collection<String> factTypeVars() {
        return factTypeVars;
    }

    public FactBuilder[] parsedFactTypes(Class<?> type) {
        List<FactBuilder> l = new ArrayList<>(factTypeVars.size());

        for (String var : this.factTypeVars) {
            l.add(FactBuilder.fact(var, type));
        }

        return l.toArray(new FactBuilder[0]);
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

    public String[] parsedConditions() {
        return parsedConditions;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "LiteralRule{" +
                "body='" + body + '\'' +
                ", parsedConditions=" + Arrays.toString(parsedConditions) +
                ", factTypeVars=" + factTypeVars +
                ", name='" + name + '\'' +
                '}';
    }
}
