plugins {
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    //withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.register("moduleJavadoc") {
    doLast {
        val coreModuleName = "org.evrete.core"
        val ajrModuleName = "org.evrete.dsl.java"

        val javadocLauncher = javaToolchains.javadocToolFor {
            languageVersion = JavaLanguageVersion.of(17)
        }.get().executablePath.asFile.absolutePath

        val buildRoot = project.layout.buildDirectory.get()
        val coreModuleSource = project.java.sourceSets.main.get().java.srcDirs.iterator().next().absolutePath
        val ajrModuleSource = project(":evrete-dsl-java").java.sourceSets.main.get().java.srcDirs.iterator().next().absolutePath
        val docFolder = "${buildRoot.dir("docs").dir("javadoc")}"

        exec {
            @Suppress("SpellCheckingInspection")
            commandLine(
                    javadocLauncher,
                    "-Xdoclint:none", //TODO uncomment and address warnings
                    "-quiet",
                    "-d",
                    docFolder,
                    "-notree",
                    "-nohelp",
                    "--module-source-path",
                    "${coreModuleName}=${coreModuleSource}",
                    "--module-source-path",
                    "${ajrModuleName}=${ajrModuleSource}",
                    "--module",
                    "${coreModuleName},${ajrModuleName}"
            )
        }
    }
}

tasks.register<Jar>("moduleJavadocJar") {
    val buildRoot = project.layout.buildDirectory.get()
    val docFolder = "${buildRoot.dir("docs").dir("javadoc")}"
    val jarFolder = "${buildRoot.dir("libs")}"
    from(docFolder)

    archiveBaseName = project.name
    archiveClassifier.set("javadoc")
    destinationDirectory.set(file(jarFolder))
}

tasks.named("moduleJavadoc") {
    finalizedBy("moduleJavadocJar")
}

tasks.named("compileJava") {
    finalizedBy("moduleJavadoc")
}


tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

val mavenVersion = rootProject.version.toString()
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                group = "org.evrete"
                artifactId = project.name
                version = mavenVersion
                withXml {
                    asNode().appendNode("parent").apply {
                        appendNode("groupId", "org.evrete")
                        appendNode("artifactId", "evrete")
                        appendNode("version", mavenVersion)
                    }
                }
            }

            from(components["java"])
            // Add Javadoc artifact
            artifact(tasks["moduleJavadocJar"])
        }
    }
    repositories {
        mavenLocal()
        mavenCentral {
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("mavenCentralUserName") as String? ?: ""
                password = findProperty("mavenCentralPassword") as String? ?: ""
            }
        }
    }
}

