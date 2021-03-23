package org.evrete;

import org.evrete.api.RuleScope;

import java.security.*;
import java.util.EnumMap;

/**
 * <p>
 * Whether it's a condition (LHS), or rule action (RHS), or both, Evrete rule engine heavily relies
 * on compiling Java sources on the fly or building rules from external Java classes. To prevent
 * potential malicious rules authored by third-parties, generated rule sources are split into three
 * categories, each having its own security permissions.
 * </p>
 * <p>
 * The {@link RuleScope#LHS} scope holds all permissions related to the evaluation of literal conditions
 * and field values. For example, if evaluation of a condition requires file or network access,
 * necessary permissions should be added to this scope <strong>before</strong> the condition is compiled.
 * </p>
 * <p>
 * The {@link RuleScope#RHS} scope holds all permissions related to the action part of a rule (right-hand side)
 * </p>
 * <p>
 * The {@link RuleScope#BOTH} is used when both LHS and RHS sides come from the same source.
 * </p>
 */
public class SourceSecurity {
    private static final CodeSource CODE_SOURCE = new CodeSource(null, (CodeSigner[]) null);
    private final EnumMap<RuleScope, ProtectionDomain> protectionDomains = new EnumMap<>(RuleScope.class);
    private final EnumMap<RuleScope, Permissions> permissions = new EnumMap<>(RuleScope.class);

    SourceSecurity() {
        for (RuleScope scope : RuleScope.values()) {
            permissions.put(scope, new Permissions());
        }
    }

    /**
     * <p>
     * Adds security permission to protection scope. <strong>Important: </strong>
     * when a ProtectionDomain is created in the given scope, the corresponding Permissions
     * becomes locked.
     * </p>
     *
     * @param scope      scope of the permission
     * @param permission permission to add
     * @return self
     * @throws SecurityException if the corresponded Permissions object is locked
     */
    public SourceSecurity addPermission(RuleScope scope, Permission permission) {
        permissions.get(scope).add(permission);
        if (scope == RuleScope.BOTH) {
            addPermission(RuleScope.LHS, permission);
            addPermission(RuleScope.RHS, permission);
        }
        return this;
    }

    /**
     * <p>
     * Creates if necessary and returns ProtectionDomain for s security scope
     * </p>
     *
     * @param scope scope of protection
     * @return existing or newly created ProtectionDomain
     */
    public ProtectionDomain getProtectionDomain(RuleScope scope) {
        return protectionDomains
                .computeIfAbsent(scope, s -> {
                    Permissions perms = permissions.get(s);
                    return new ProtectionDomain(CODE_SOURCE, perms);
                });
    }

}
