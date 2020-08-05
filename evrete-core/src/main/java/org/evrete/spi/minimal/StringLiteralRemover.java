package org.evrete.spi.minimal;

import java.util.HashMap;
import java.util.Map;

class StringLiteralRemover {
    private static final String PREFIX = "${const";
    private static final String SUFFIX = "}";
    private static final char[] QUOTES = new char[]{'\'', '"', '`'};


    private final String original;
    private final String converted;
    private final Map<String, String> stringConstantMap;

    private StringLiteralRemover(String original, String converted, Map<String, String> stringConstantMap) {
        this.original = original;
        this.converted = converted;
        this.stringConstantMap = stringConstantMap;
    }

    public static StringLiteralRemover of(String s) {

        // First name and replace all String constants
        int stringConstantId = 0;
        Map<String, String> stringConstantMap = new HashMap<>();
        String current = s;
        for (char quote : QUOTES) {
            int start;
            while ((start = current.indexOf(quote)) >= 0) {
                int end = current.indexOf(quote, start + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Unbalanced quote at position " + start + " in " + current);
                } else {
                    String stringConstant = current.substring(start, end + 1);
                    // Now we need to name this constant
                    String varName;
                    do {
                        varName = PREFIX + (stringConstantId++) + SUFFIX;
                    } while (current.contains(varName) || s.contains(varName));

                    stringConstantMap.put(varName, stringConstant.substring(1, stringConstant.length() - 1));
                    current = current.replace(stringConstant, varName);
                }
            }
        }

        // Now it's safe to remove all the whitespaces
        current = current.replaceAll("\\s", "");

        return new StringLiteralRemover(s, current, stringConstantMap);
    }

    public String unwrapLiterals(String arg) {
        String s = arg;
        for (Map.Entry<String, String> entry : stringConstantMap.entrySet()) {
            s = s.replace(entry.getKey(), "\"" + entry.getValue() + "\"");
        }
        return s;
    }

    public String getConverted() {
        return converted;
    }

    public String getOriginal() {
        return original;
    }

    @Override
    public String toString() {
        return "StringLiteralRemover{" +
                "original='" + original + '\'' +
                ", converted='" + converted + '\'' +
                ", stringConstantMap=" + stringConstantMap +
                '}';
    }
}
