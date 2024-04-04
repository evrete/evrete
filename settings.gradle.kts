rootProject.name = "evrete"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(
        "evrete-core",
        "evrete-core-8",
        "evrete-dsl-java",
        "evrete-dsl-java-8",
        "evrete-jsr94",
        "evrete-benchmarks",
        "evrete-code-samples"
)
