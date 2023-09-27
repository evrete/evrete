package org.evrete;

import org.evrete.api.Copyable;
import org.evrete.api.FluentImports;
import org.evrete.api.Imports;

import java.util.Properties;
import java.util.logging.Logger;

public class Configuration extends Properties implements Copyable<Configuration>, FluentImports<Configuration> {
    public static final String OBJECT_COMPARE_METHOD = "evrete.core.fact-identity-strategy";
    public static final String INSERT_BUFFER_SIZE = "evrete.core.insert-buffer-size";
    public static final String WARN_UNKNOWN_TYPES = "evrete.core.warn-unknown-types";
    public static final int INSERT_BUFFER_SIZE_DEFAULT = 4096;
    public static final String IDENTITY_METHOD_EQUALS = "equals";
    public static final String IDENTITY_METHOD_IDENTITY = "identity";
    static final String SPI_MEMORY_FACTORY = "evrete.spi.memory-factory";
    static final String SPI_EXPRESSION_RESOLVER = "evrete.spi.expression-resolver";
    static final String SPI_TYPE_RESOLVER = "evrete.spi.type-resolver";
    static final String SPI_RHS_COMPILER = "evrete.spi.rhs-compiler";
    static final String PARALLELISM = "evrete.core.parallelism";
    public static final String CONDITION_BASE_CLASS = "evrete.impl.condition-base-class";

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static final long serialVersionUID = -9015471049604658637L;
    private final Imports imports;

    @SuppressWarnings("unused")
    public Configuration(Properties defaults) {
        for (String key : defaults.stringPropertyNames()) {
            setProperty(key, defaults.getProperty(key));
        }
        this.imports = new Imports();
    }

    private Configuration(Properties defaults, Imports imports) {
        for (String key : defaults.stringPropertyNames()) {
            setProperty(key, defaults.getProperty(key));
        }
        this.imports = imports.copyOf();
    }

    public Configuration() {
        super(System.getProperties());

        setIfAbsent(WARN_UNKNOWN_TYPES, Boolean.TRUE.toString());
        setIfAbsent(OBJECT_COMPARE_METHOD, IDENTITY_METHOD_IDENTITY);
        setIfAbsent(INSERT_BUFFER_SIZE, String.valueOf(INSERT_BUFFER_SIZE_DEFAULT));
        this.imports = new Imports();
    }

    private void setIfAbsent(String key, String value) {
        if (!contains(key)) {
            setProperty(key, value);
        }
    }

    public boolean getAsBoolean(String property) {
        return Boolean.parseBoolean(getProperty(property));
    }

    public boolean getAsBoolean(String property, boolean defaultValue) {
        String prop = getProperty(property, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(prop);
    }

    public int getAsInteger(String property, int defaultValue) {
        String val = getProperty(property);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            LOGGER.warning("Property '" + property + "' is not an integer, returning default value of " + defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Imports getImports() {
        return imports;
    }

    public final Configuration addImport(String imp) {
        this.imports.add(imp);
        return this;
    }

    @Override
    public Configuration copyOf() {
        return new Configuration(this, this.imports);
    }
}
/*
!!!! evrete.spi.lhs-compiler.strip-whitespaces: {java.specification.version=21, sun.jnu.encoding=UTF-8, java.class.path=/Users/andbi/.m2/repository/org/junit/platform/junit-platform-launcher/1.9.2/junit-platform-launcher-1.9.2.jar:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar:/Applications/IntelliJ IDEA.app/Contents/plugins/junit/lib/junit5-rt.jar:/Applications/IntelliJ IDEA.app/Contents/plugins/junit/lib/junit-rt.jar:/Users/andbi/work/contribute/evrete-project/cloned/evrete/evrete-core/target/test-classes:/Users/andbi/work/contribute/evrete-project/cloned/evrete/evrete-core/target/classes:/Users/andbi/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.9.2/junit-jupiter-api-5.9.2.jar:/Users/andbi/.m2/repository/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar:/Users/andbi/.m2/repository/org/junit/platform/junit-platform-commons/1.9.2/junit-platform-commons-1.9.2.jar:/Users/andbi/.m2/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar:/Users/andbi/.m2/repository/org/junit/jupiter/junit-jupiter-engine/5.9.2/junit-jupiter-engine-5.9.2.jar:/Users/andbi/.m2/repository/org/junit/platform/junit-platform-engine/1.9.2/junit-platform-engine-1.9.2.jar:/Users/andbi/.m2/repository/org/junit/jupiter/junit-jupiter-params/5.9.2/junit-jupiter-params-5.9.2.jar, java.vm.vendor=Oracle Corporation, sun.arch.data.model=64, idea.test.cyclic.buffer.size=8388608, java.vendor.url=https://java.oracle.com/, property=java.util.logging.config.filesrc/test/resources/logging.properties, os.name=Mac OS X, java.vm.specification.version=21, sun.java.launcher=SUN_STANDARD, user.country=US, sun.boot.library.path=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/lib, sun.java.command=com.intellij.rt.junit.JUnitStarter -ideVersion5 -junit5 org.evrete.spi.minimal.ExpressionsTest,test3, http.nonProxyHosts=local|*.local|169.254/16|*.169.254/16, jdk.debug=release, sun.cpu.endian=little, user.home=/Users/andbi, user.language=en, sun.stderr.encoding=UTF-8, java.specification.vendor=Oracle Corporation, java.version.date=2023-09-19, java.home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home, file.separator=/, evrete.core.insert-buffer-size=4096, java.vm.compressedOopsMode=Zero based, sun.stdout.encoding=UTF-8, line.separator=
, java.vm.specification.vendor=Oracle Corporation, java.specification.name=Java Platform API Specification, apple.awt.application.name=JUnitStarter, evrete.core.fact-identity-strategy=identity, evrete.spi.lhs-compiler.strip-whitespaces=false, sun.management.compiler=HotSpot 64-Bit Tiered Compilers, ftp.nonProxyHosts=local|*.local|169.254/16|*.169.254/16, java.runtime.version=21+35-LTS-2513, user.name=andbi, stdout.encoding=UTF-8, path.separator=:, os.version=13.6, java.runtime.name=Java(TM) SE Runtime Environment, file.encoding=UTF-8, java.vm.name=Java HotSpot(TM) 64-Bit Server VM, java.vendor.url.bug=https://bugreport.java.com/bugreport/, java.io.tmpdir=/var/folders/vz/dk2_lgys0nng3z9f52gbktm80000gn/T/, java.version=21, user.dir=/Users/andbi/work/contribute/evrete-project/cloned/evrete/evrete-core, os.arch=x86_64, java.vm.specification.name=Java Virtual Machine Specification, native.encoding=UTF-8, java.library.path=/Users/andbi/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:., java.vm.info=mixed mode, sharing, stderr.encoding=UTF-8, java.vendor=Oracle Corporation, java.vm.version=21+35-LTS-2513, sun.io.unicode.encoding=UnicodeBig, evrete.core.warn-unknown-types=true, socksNonProxyHosts=local|*.local|169.254/16|*.169.254/16, java.class.version=65.0}

 */