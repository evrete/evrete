package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.events.ContextEvent;
import org.evrete.runtime.compiler.RuntimeClassloader;
import org.evrete.runtime.compiler.SourceCompiler;
import org.evrete.util.AbstractEnvironment;

/**
 * <p>
 * Runtime context's root class containing the very basic information about the runtime context.
 * </p>
 *
 * @param <C> contact type
 */
abstract class AbstractRuntimeBase<C extends RuntimeContext<C>> extends AbstractEnvironment implements RuntimeContext<C> {
    private final Imports imports;
    private RuntimeClassloader classloader;
    private final TypeResolver typeResolver;
    private final KnowledgeService service;
    private final EventMessageBus messageBus;

    AbstractRuntimeBase(KnowledgeService service) {
        super(service.getConfiguration());
        this.service = service;
        this.imports = service.getConfiguration().getImports().copyOf();
        RuntimeClassloader runtimeClassloader = new RuntimeClassloader(service.getClassLoader());
        this.classloader = runtimeClassloader;
        this.typeResolver = service.getTypeResolverProvider().instance(runtimeClassloader);
        this.messageBus = service.getMessageBus().copyOf(); // Service also contains
    }

    AbstractRuntimeBase(AbstractRuntimeBase<?> parent) {
        super(parent);
        this.imports = parent.imports.copyOf();
        this.service = parent.service;
        // Isolate the classloader
        RuntimeClassloader newClassloader = new RuntimeClassloader(parent.classloader);
        this.classloader = newClassloader;
        this.typeResolver = parent.typeResolver.copy(newClassloader);
        this.messageBus = parent.messageBus.copyOf();
    }

    abstract void _assertActive();


    EventMessageBus getMessageBus() {
        return messageBus;
    }

    protected <E extends ContextEvent> void broadcast(Class<E> type, E event) {
        this.messageBus.broadcast(type, event);
    }


    @Override
    public <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass) {
        return messageBus.getPublisher(eventClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C set(String property, Object value) {
        super.set(property, value);
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public final C addImport(String imp) {
        this.imports.add(imp);
        return (C) this;
    }

    @Override
    public final Imports getImports() {
        return imports;
    }

    @Override
    public TypeResolver getTypeResolver() {
        return this.typeResolver;
    }

    @Override
    public final RuntimeClassloader getClassLoader() {
        return classloader;
    }

    @Override
    public Class<?> addClass(String binaryName, byte[] classBytes) {
        classloader.saveClass(binaryName, classBytes);
        try {
            return classloader.loadClass(binaryName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class that has been just added: " + binaryName, e);
        }
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classloader = new RuntimeClassloader(classLoader);
    }

    @Override
    public final JavaSourceCompiler getSourceCompiler() {
        return new SourceCompiler(classloader);
    }

    @Override
    public KnowledgeService getService() {
        return service;
    }

}
