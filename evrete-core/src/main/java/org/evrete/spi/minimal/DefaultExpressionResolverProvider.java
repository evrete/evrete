package org.evrete.spi.minimal;

import org.evrete.api.ExpressionResolver;
import org.evrete.api.RuleScope;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.ExpressionResolverProvider;

import java.security.ProtectionDomain;

public class DefaultExpressionResolverProvider extends LeastImportantServiceProvider implements ExpressionResolverProvider {

    @Override
    public ExpressionResolver instance(RuntimeContext<?> requester) {

        ProtectionDomain domain = requester.getService().getSecurity().getProtectionDomain(RuleScope.LHS);
        return new DefaultExpressionResolver(requester, getCreateJavaCompiler(requester, domain));
    }
}
