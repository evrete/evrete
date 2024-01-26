package org.evrete.runtime.compiler;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.IOUtils;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class PackageExplorer {
    private static final String CLASS_FILE_EXTENSION = ".class";
    private static final String CLASS_MODULE_INFO = "module-info.class";
    private final Map<String, Collection<JavaFileObject>> cache = new HashMap<>();

    private final RuntimeClassloader classLoader;

    public PackageExplorer(RuntimeClassloader classLoader) {
        this.classLoader = classLoader;
    }

    private static Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null)
            throw new IllegalArgumentException("Missing scheme");

        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file"))
            return FileSystems.getDefault().provider().getPath(uri);

        // try to find provider
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }

        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }

    private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL) {
        String key = packageName + packageFolderURL;
        Collection<JavaFileObject> cached = cache.get(key);
        if (cached == null) {
            cached = new LinkedList<>(listUnderUncached(packageName, packageFolderURL));
            cache.put(key, cached);
        }
        return cached;
    }

    private Collection<JavaFileObject> listUnderUncached(String packageName, URL packageURL) {
        Collection<JavaFileObject> result = null;


        URLConnection connection;
        try {
            connection = packageURL.openConnection();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }


        if (connection instanceof JarURLConnection) {
            // Read from JarURLConnection
            return asUrlConnection((JarURLConnection) connection);
        }

        // Try as a filesystem resource
        result = asFileResource(packageName, packageURL);


        if (result == null) {
            // Try as zip input stream (suitable for various virtual filesystems like WildFly's VFS)
            try (InputStream stream = connection.getInputStream()) {
                if (stream instanceof ZipInputStream) {
                    ZipInputStream zis = (ZipInputStream) stream;
                    result = asZipInputStream(packageName, zis);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        if (result == null) {
            throw new IllegalStateException("Unknown resource type: " + packageURL);
        } else {
            return result;
        }
    }

    @Nullable
    private Collection<JavaFileObject> asFileResource(String packageName, URL packageFolderURL) {
        try (Stream<Path> stream = Files.walk(uriToPath(packageFolderURL.toURI()), 1)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(CLASS_FILE_EXTENSION))
                    .map((Function<Path, JavaFileObject>) path -> {
                        String fileName = path
                                .getFileName()
                                .toString();
                        fileName = fileName.substring(0, fileName.length() - CLASS_FILE_EXTENSION.length());
                        String binaryName = packageName + '.' + fileName;
                        return new ClassPathSource(binaryName, path.toUri());
                    })
                    .collect(Collectors.toList());
        } catch (FileSystemNotFoundException | URISyntaxException | IOException ignored) {
            // Failed to read classes, probably not a filesystem resources
            return null;
        }
    }

    @NonNull
    private Collection<JavaFileObject> asUrlConnection(JarURLConnection jarConn) {
        try {

            String rootEntryName = jarConn.getEntryName();
            JarFile jarFile = jarConn.getJarFile();

            return jarFile.stream()
                    .filter(e -> {
                        String entryName = e.getName();
                        return
                                !e.isDirectory()
                                        && rootEntryName != null
                                        && entryName.startsWith(rootEntryName)
                                        && entryName.endsWith(CLASS_FILE_EXTENSION)
                                        && !entryName.endsWith(CLASS_MODULE_INFO)
                                        && entryName.indexOf('/', rootEntryName.length() + 1) == -1
                                ;
                    })
                    .map((Function<JarEntry, JavaFileObject>) entry -> {
                        String name = entry.getName();
                        String fileName = name.replaceAll("/", ".");
                        fileName = fileName.substring(0, fileName.length() - CLASS_FILE_EXTENSION.length());

                        byte[] bytes = IOUtils.bytes(jarFile, entry);
                        try {
                            return new ClassPathJavaObject(Class.forName(fileName, false, classLoader), bytes);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collection<JavaFileObject> asZipInputStream(String packageName, ZipInputStream zis) {
        ZipEntry entry;
        Collection<JavaFileObject> result = new LinkedList<>();
        try {
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    if (entryName.endsWith(CLASS_FILE_EXTENSION) && entryName.indexOf('/') < 0 && !entryName.endsWith(CLASS_MODULE_INFO)) {
                        String classEntry = entryName.substring(0, entryName.length() - CLASS_FILE_EXTENSION.length());
                        String className = packageName + "." + classEntry;


                        byte[] bytes = IOUtils.toByteArray(zis);
                        ClassPathJavaObject compiledClass = new ClassPathJavaObject(Class.forName(className, false, classLoader), bytes);
                        result.add(compiledClass);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

    }

    List<JavaFileObject> find(String packageName) throws IOException {
        String javaPackageName = packageName.replaceAll("\\.", "/");
        List<JavaFileObject> result = new ArrayList<>();
        Enumeration<URL> resources = classLoader.getResources(javaPackageName);
        while (resources.hasMoreElements()) {
            // one URL for each jar on the classpath that has the given package
            URL url = resources.nextElement();
            result.addAll(listUnder(packageName, url));
        }
        return result;
    }
}