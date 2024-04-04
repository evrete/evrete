@file:Suppress("VulnerableLibrariesLocal")

val droolsVersion = "9.44.0.Final"
val jmhVersion = "1.37"
val slf4jVersion = "2.0.3"

plugins {
    id("java-library")
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":evrete-core"))
    implementation("org.drools:drools-core:$droolsVersion")
    implementation("org.drools:drools-mvel:$droolsVersion")
    implementation("org.drools:drools-compiler:$droolsVersion")
    implementation("org.openjdk.jmh:jmh-generator-annprocess:${jmhVersion}")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:${jmhVersion}")

    // Drools logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
}


tasks.register<JavaExec>("expressions") {
    mainClass = "org.evrete.benchmarks.ExpressionsBenchmarks"
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("hashCollections") {
    mainClass = "org.evrete.benchmarks.HashCollectionsBenchmarks"
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("linkedCollections") {
    mainClass = "org.evrete.benchmarks.LinkedCollections"
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("ruleEngines") {
    mainClass = "org.evrete.benchmarks.RuleEngines"
    classpath = sourceSets.main.get().runtimeClasspath
    args("${project.projectDir}/src/main/resources")
}
