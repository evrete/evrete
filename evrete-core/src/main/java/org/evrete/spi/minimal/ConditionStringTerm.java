package org.evrete.spi.minimal;

import org.evrete.api.FieldReference;
import org.evrete.util.NextIntSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConditionStringTerm extends FieldReferenceImpl {
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[_a-zA-Z][_a-zA-Z0-9]*)*");

    final int start;
    final int end;
    final String varName;

    ConditionStringTerm(int start, int end, FieldReference delegate, NextIntSupplier fieldCounter) {
        super(delegate);
        this.start = start;
        this.end = end;
        this.varName = "var" + fieldCounter.next();
    }

    ConditionStringTerm(int start, int end, ConditionStringTerm existing) {
        super(existing);
        this.start = start;
        this.end = end;
        this.varName = existing.varName;
    }

    private static ConditionStringTerm resolveTerm(int start, int actualEnd, FieldReference ref, NextIntSupplier fieldCounter, List<ConditionStringTerm> terms) {
        // Scanning existing terms
        for (ConditionStringTerm t : terms) {
            if (t.type().equals(ref.type()) && t.field().equals(ref.field())) {
                // Found the same reference
                return new ConditionStringTerm(start, actualEnd, t);
            }
        }
        return new ConditionStringTerm(start, actualEnd, ref, fieldCounter);
    }

    static List<ConditionStringTerm> resolveTerms(StringLiteralEncoder.Encoded encoded, Function<String, FieldReference> resolver) {
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

            String s = expression.substring(start, actualEnd);
            FieldReference fieldReference = resolver.apply(s);


            ConditionStringTerm t = resolveTerm(start, actualEnd, fieldReference, fieldCounter, terms);
            terms.add(t);
        }
        return  terms;
    }
}
