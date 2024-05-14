package org.evrete.dsl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class WrappedClass {
    protected final Class<?> delegate;
    protected final Method[] publicMethods;

    WrappedClass(Class<?> delegate) {
        this.delegate = delegate;
        this.publicMethods = delegate.getMethods();
    }

    protected WrappedClass(WrappedClass other) {
        this.delegate = other.delegate;
        this.publicMethods = other.publicMethods;
    }

    private boolean isExtendedBy(WrappedClass other) {
        return this != other && this.delegate.isAssignableFrom(other.delegate);
    }

    public static List<RulesClass> excludeSubclasses(List<WrappedClass> classes) {
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        List<RulesClass> result = new ArrayList<>(classes.size());
        for (WrappedClass wrappedClass : classes) {
            boolean isSubclassed = false;
            for (WrappedClass potentialSubclass : classes) {
                if(wrappedClass.isExtendedBy(potentialSubclass)) {
                    isSubclassed = true;
                    break;
                }
            }

            if(!isSubclassed) {
                int modifiers = wrappedClass.delegate.getModifiers();
                // Check if the class is public and not abstract
                boolean isPublic = Modifier.isPublic(modifiers);
                boolean isNotAbstract = !Modifier.isAbstract(modifiers);
                if(isPublic && isNotAbstract) {
                    result.add(new RulesClass(wrappedClass, publicLookup));
                }
            }
        }

        return result;
    }
}
