package org.evrete.runtime.compiler;

import org.evrete.api.LhsField;
import org.evrete.util.NextIntSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConditionStringTerm {
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[_a-zA-Z][_a-zA-Z0-9]*)*");

    final int start;
    final int end;
    final String varName;
    final LhsField<String, String> ref;

    ConditionStringTerm(int start, int end, LhsField<String, String> ref, NextIntSupplier fieldCounter) {
        this.start = start;
        this.end = end;
        this.varName = "var" + fieldCounter.incrementAndGet();
        this.ref = ref;
    }

    ConditionStringTerm(int start, int end, ConditionStringTerm existing) {
        this.start = start;
        this.end = end;
        this.varName = existing.varName;
        this.ref = existing.ref;
    }

    private static ConditionStringTerm resolveTerm(int start, int end, LhsField<String, String> ref, NextIntSupplier fieldCounter, List<ConditionStringTerm> terms) {
        // Scanning existing terms
        for (ConditionStringTerm t : terms) {
            if (t.ref.equals(ref)) {
                // Found the same reference
                return new ConditionStringTerm(start, end, t);
            }
        }
        return new ConditionStringTerm(start, end, ref, fieldCounter);
    }

    static List<ConditionStringTerm> resolveTerms(StringLiteralEncoder.Encoded encoded) {
        final String expression = encoded.value;

        Matcher m = REFERENCE_PATTERN.matcher(expression);
        List<ConditionStringTerm> terms = new ArrayList<>();

        NextIntSupplier fieldCounter = new NextIntSupplier();
        while (m.find()) {
            int start = m.start(), end = m.end(), actualEnd = end;
            if (end < expression.length() && expression.charAt(end) == '(') {
                // The last group is a method call that needs to be effectively stripped off
                // by moving the actualEnd to the rightmost dot
                actualEnd = expression.substring(start, end).lastIndexOf('.') + start;
            }

            String matched = expression.substring(start, actualEnd);
            final LhsField<String, String> fieldReference = LhsField.parseDottedVariable(matched);


            ConditionStringTerm t = resolveTerm(start, actualEnd, fieldReference, fieldCounter, terms);
            terms.add(t);
        }
        return  terms;
    }

    @Override
    public String toString() {
        return "ConditionStringTerm{" +
                "start=" + start +
                ", end=" + end +
                ", varName='" + varName + '\'' +
                '}';
    }
}
