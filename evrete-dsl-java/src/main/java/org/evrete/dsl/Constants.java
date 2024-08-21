package org.evrete.dsl;

/**
 * Constants interface defines the constant values for different providers.
 */
public interface Constants {
    /**
     * Name of the Java Source DSL provider
     */
    String PROVIDER_JAVA_SOURCE = "JAVA-SOURCE";

    /**
     * Name of the Java Cass DSL provider
     */
    String PROVIDER_JAVA_CLASS = "JAVA-CLASS";

    /**
     * Name of the Java Jar DSL provider
     */
    String PROVIDER_JAVA_JAR = "JAVA-JAR";

    /**
     * A boolean property that defines whether internal Java sources related to
     * literal conditions should extend the corresponding Java class with annotated
     * rules. The default value is <code>true</code>.
     */
    String PROP_EXTEND_RULE_CLASSES = "org.evrete.dsl.extend-rule-classes";

    /**
     * A property that specifies the character encoding to be used for
     * Java sources.
     */
    String PROP_SOURCES_CHARSET = "org.evrete.dsl.sources-charset";

    /**
     * The default character encoding for internal Java sources.
     */
    String PROP_SOURCES_CHARSET_DEFAULT = "UTF-8";

    /**
     * A property that specifies the class names to import from a JAR archive.
     */
    String PROP_RULE_CLASSES = "org.evrete.dsl.rule-classes";

    /**
     * A property that specifies the names of the rulesets to import from a JAR archive.
     */
    String PROP_RULESETS = "org.evrete.dsl.ruleset-names";

}
