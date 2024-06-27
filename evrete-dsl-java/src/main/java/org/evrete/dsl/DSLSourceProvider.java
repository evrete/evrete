package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.util.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-SOURCE' DSL knowledge.
 */
public class DSLSourceProvider extends AbstractDSLProvider {
    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[] {
            TYPE_INPUT_STREAM,
            TYPE_URL,
            TYPE_READER,
            TYPE_CHAR_SEQUENCE,
            TYPE_FILE
    };
    /**
     * Default public constructor
     */
    public DSLSourceProvider() {
    }

    @Override
    public Set<Class<?>> sourceTypes() {
        return Set.of(SUPPORTED_TYPES);
    }


    @Override
    public String getName() {
        return PROVIDER_JAVA_SOURCE;
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, URL resource) throws IOException {
        Charset charset = charset(target.getContext().getConfiguration());
        String source = toSourceString(charset, resource);
        return createClassMetaFromSource(source);
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, Reader resource) throws IOException {
        String source = toSourceString(resource);
        return createClassMetaFromSource(source);
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, CharSequence resource) {
        String source = resource.toString();
        return createClassMetaFromSource(source);
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, InputStream resource) throws IOException {
        Charset charset = charset(target.getContext().getConfiguration());
        String source = toSourceString(charset, resource);
        return this.createClassMetaFromSource(source);
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, File resource) throws IOException {
        return createClassMeta(target, resource.toURI().toURL());
    }

    private <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMetaFromSource(String source) {
        return List.of(new DSLMetaLiteralSource<>(publicLookup, source));
    }

    private static String toSourceString(Reader reader) throws IOException {
        return CommonUtils.toString(reader);
    }

    private static String toSourceString(Charset charset, InputStream stream) throws IOException {
        return new String(CommonUtils.toByteArrayChecked(stream), charset);
    }

    private static String toSourceString(Charset charset, URL url) throws IOException {
        try(InputStream is = url.openStream()) {
            return toSourceString(charset, is);
        }
    }

}
