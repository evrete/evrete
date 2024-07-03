package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;

public abstract class AbstractActiveRule<FG extends KnowledgeFactGroup, LHS extends ActiveLhs<FG>, R extends AbstractRuntime<?, ?>> extends AbstractRule {
    private final R runtime;
    private final LHS lhs;
    private final AbstractRule parent;

    /**
     * This constructor will be called by the {@link SessionRule} class.
     *
     * @param runtime session runtime
     * @param knowledgeRule parent knowledge rule
     * @param lhs session LHS
     */
    AbstractActiveRule(R runtime, KnowledgeRule knowledgeRule, LHS lhs) {
        this(runtime, knowledgeRule, knowledgeRule.getSalience(), lhs);
    }

    AbstractActiveRule(R runtime, AbstractRule other, int salience, LHS lhs) {
        super(other, other.getName(), salience);
        this.runtime = runtime;
        this.lhs = lhs;
        this.parent = other;
    }

    final LHS getLhs() {
        return lhs;
    }

    @Override
    @NonNull
    public NamedType resolve(@NonNull String var) {
        return parent.resolve(var);
    }

    @Override
    public Collection<NamedType> getDeclaredFactTypes() {
        return parent.getDeclaredFactTypes();
    }


    public final R getRuntime() {
        return runtime;
    }

    @Override
    public final void setRhs(String literalRhs) {
        if (literalRhs != null) {
            setRhs(runtime.compileRHS(literalRhs, this));
        }
    }
}
