package org.evrete.dsl;

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

        List<Class<?>> classes = new ArrayList<>(compile(sources));
        classes.sort(Comparator.comparing(Class::getName));

        File out = File.createTempFile("evrete-test", ".jar");
        FileOutputStream fos = new FileOutputStream(out);
        JarOutputStream  jar = new JarOutputStream(fos);
        Set<String> createdDirs = new HashSet<>();
        for(Class<?> c : classes) {
            addClassToJar(c, jar, createdDirs);
        }
        jar.close();

        try {
            jarConsumer.accept(out);
        } finally {
            Files.deleteIfExists(out.toPath());
        }
    }

    private static void addClassToJar(Class<?> clazz, JarOutputStream jos, Set<String> createdDirs) throws IOException {
        // Get the class bytecode
        byte[] classBytes = readClassBytes(clazz);

        // Construct the entry name
        String className = clazz.getName().replace('.', '/') + ".class";

        // Create directory entries if necessary
        String dirName = className.substring(0, className.lastIndexOf('/') + 1);
        createDirectoryEntries(dirName, jos, createdDirs);

        // Create and add the jar entry for the class
        JarEntry jarEntry = new JarEntry(className);
        jos.putNextEntry(jarEntry);
        // Write the class bytes to the JAR entry
        jos.write(classBytes);

        // Close the entry
        jos.closeEntry();
    }

    private static void createDirectoryEntries(String dirName, JarOutputStream jos, Set<String> createdDirs) throws IOException {
        if (dirName.isEmpty() || createdDirs.contains(dirName)) {
            return;
        }

        // Ensure parent directories are created first
        int lastSlashIndex = dirName.lastIndexOf('/', dirName.length() - 2);
        if (lastSlashIndex >= 0) {
            createDirectoryEntries(dirName.substring(0, lastSlashIndex + 1), jos, createdDirs);
        }

        // Create the current directory entry
        JarEntry dirEntry = new JarEntry(dirName);
        jos.putNextEntry(dirEntry);
        jos.closeEntry();
        createdDirs.add(dirName);
    }


    public static byte[] readClassBytes(Class<?> cls) throws IOException {
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
