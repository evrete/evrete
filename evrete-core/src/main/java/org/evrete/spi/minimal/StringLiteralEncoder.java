package org.evrete.spi.minimal;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * This encoder parses String arguments and replaces each quoted string fragment with a unique character sequence.
 * The resulting String can be modified by other tools, and if the modification doesn't affect the encoded keys,
 * the quoted fragments can be restored.
 * </p>
 * <p>
 * In fact, this class is a very basic tool for parsing Java literal sources. More advanced implementations
 * of the library's Service Provider Interface (SPI) may use more sophisticated tools like ANTLR.
 * </p>
 */
final class StringLiteralEncoder {
    private static final String PREFIX = "${const";
    private static final String SUFFIX = "}";
    private static final char[] QUOTES = new char[]{'\'', '"', '`'};

    private final String original;
    private final Encoded encoded;
    private final Map<String, String> stringConstantMap;

    private StringLiteralEncoder(String original, Encoded encoded, Map<String, String> stringConstantMap) {
        this.original = original;
        this.encoded = encoded;
        this.stringConstantMap = stringConstantMap;
    }

    static StringLiteralEncoder of(String s, boolean stripWhiteSpaces) {

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
        if (stripWhiteSpaces) {
            current = current.replaceAll("\\s", "");
        } else {
            current = current.replaceAll("\\s{2,}", " ");
        }

        return new StringLiteralEncoder(s, new Encoded(current), stringConstantMap);
    }

    @SuppressWarnings("unused")
    public Map<String, String> getConstantMap() {
        return stringConstantMap;
    }

    public String unwrapLiterals(final String arg) {
        String s = arg;
        for (Map.Entry<String, String> entry : stringConstantMap.entrySet()) {
            s = s.replace(entry.getKey(), "\"" + entry.getValue() + "\"");
        }
        return s;
    }

    public Encoded getEncoded() {
        return encoded;
    }

    @Override
    public String toString() {
        return "StringLiteralRemover{" +
                "original='" + original + '\'' +
                ", converted='" + encoded + '\'' +
                ", stringConstantMap=" + stringConstantMap +
                '}';
    }

    static final class Encoded {
        final String value;

        public Encoded(String value) {
            this.value = value;
        }
    }
}
