package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.api.spi.SourceCompiler;
import org.evrete.api.spi.SourceCompilerProvider;
import org.evrete.util.JavaSourceUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class TestUtils {

    public static File testResourceAsFile(String path) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if(url == null) {
            throw new IOException("Resource not found: " + path);
        } else {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
    }

    static synchronized void createTempJarFile(File root, Consumer<File> jarConsumer) throws Exception {
        Path compileRoot = root.toPath();

        Stream<Path> javaFiles = Files.find(compileRoot, Integer.MAX_VALUE, (path, attrs) -> path.toString().endsWith(".java"));

        Set<String> sources = javaFiles.map(path -> {
            try {
                //noinspection ReadWriteStringCanBeUsed
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(Collectors.toSet());

        Collection<Class<?>> classes = compile(sources);

        File out = File.createTempFile("evrete-test", ".jar");
        FileOutputStream fos = new FileOutputStream(out);
        JarOutputStream  jar = new JarOutputStream(fos);
        for(Class<?> c : classes) {
            String binaryName = c.getName().replaceAll("\\.", "/");
            String name = binaryName + ".class";
            ZipEntry zipEntry = new JarEntry(name);
            jar.putNextEntry(zipEntry);

            byte[] classBytes = getClassBytes(c);
            InputStream stream = new ByteArrayInputStream(classBytes);
            copy(stream, jar);
            stream.close();
            jar.closeEntry();
        }
        jar.close();

        try {
            jarConsumer.accept(out);
        } finally {
            Files.deleteIfExists(out.toPath());
        }
    }

    public static byte[] getClassBytes(Class<?> cls) throws IOException {
        // Convert class reference to resource path
        String resourcePath = cls.getName().replace('.', '/') + ".class";

        // Get the class loader of the class
        ClassLoader classLoader = cls.getClassLoader();

        // Load the class file as resource stream
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class not found: " + cls.getName());
            }

            // Read the class bytes
            return inputStream.readAllBytes();
        }
    }

    private static SourceCompiler createSourceCompiler() {
        return ServiceLoader.load(SourceCompilerProvider.class).iterator().next().instance(Thread.currentThread().getContextClassLoader());
    }

    static Collection<Class<?>> compile(Collection<String> sources) throws Exception {
        SourceCompiler sourceCompiler = createSourceCompiler();
        Collection<SourceCompiler.ClassSource> resolved = sources.stream().map(JavaSourceUtils::parse).collect(Collectors.toList());

        Collection<SourceCompiler.Result<SourceCompiler.ClassSource>> compiled =  sourceCompiler.compile(resolved);
        return compiled.stream().map((Function<SourceCompiler.Result<SourceCompiler.ClassSource>, Class<?>>) SourceCompiler.Result::getCompiledClass).collect(Collectors.toList());
    }


    private static void copy(InputStream source, OutputStream sink) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
    }

    public static class EnvHelperData {
        private static final Map<String, List<Object>> data = new HashMap<>();
        private static int count = 0;

        public static void reset() {
            data.clear();
            count = 0;
        }

        public static void add(String property, Object val) {
            data.computeIfAbsent(property, k -> new ArrayList<>()).add(val);
            count++;
        }

        public static void add(EnvironmentChangeEvent e) {
            data.computeIfAbsent(e.getProperty(), k -> new ArrayList<>()).add(e.getValue());
            count++;
        }

        static int total() {
            return count;
        }

        static int total(String property) {
            List<Object> l = data.get(property);
            return l == null ? 0 : l.size();
        }

    }

    public static class PhaseHelperData {
        static final Map<Class<?>, AtomicInteger> EVENTS = new HashMap<>();

        static {
            reset();
        }

        public static void reset() {
            EVENTS.clear();
        }

        public static int count(Class<?> event) {
            int result = 0;
            for(Map.Entry<Class<?>, AtomicInteger> entry : EVENTS.entrySet()) {
                if(event.isAssignableFrom(entry.getKey())) {
                    result += entry.getValue().intValue();
                }
            }
            return result;
        }

        public static void event(ContextEvent... events) {
            for (ContextEvent evt : events) {
                EVENTS.computeIfAbsent(evt.getClass(), k->new AtomicInteger()).incrementAndGet();
            }
        }
    }
}
