import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("maven-publish")
    id("java-library") // TODO remove from the top-level project!
    id("signing")
}

val defaultJavaVersion = 11

// Applying common settings to all projects
allprojects {
    group = "org.evrete"
    version = "3.2.00"

    apply(plugin = "java-library")

    //TODO remove this block?
    repositories {
        // Use Maven Central for resolving dependencies.
        mavenCentral()
        mavenLocal()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // This needs to be overwritten by Java8 subprojects
    the<JavaPluginExtension>()
            .toolchain
            .languageVersion
            .set(JavaLanguageVersion.of(defaultJavaVersion))
}

/*
 *  Configure Javadoc
 */
subprojects {
    tasks.withType<Javadoc> {
        options {
            source = "11"
            showFromPublic()
            encoding = "UTF-8"
        }
    }
}


/*
 * Java 8 support
 */
subprojects {
    afterEvaluate {
        if (project.name.endsWith("-8")) {
            val ltsProjectName = project.name.substringBeforeLast("-8")

            java {
                toolchain.languageVersion.set(JavaLanguageVersion.of(8))
            }

            // 1. Add additional source roots (We can not reuse the LTS ones, so we'll just copy them)
            plugins.withType<JavaPlugin> {
                sourceSets {
                    main {
                        java.srcDirs("src-lts/main/java", "src/main/java")
                        resources {
                            srcDirs("src-lts/main/resources", "src/main/resources")
                        }
                    }
                    test {
                        java.srcDirs("src-lts/test/java", "src/test/java")
                        resources {
                            srcDirs("src-lts/test/resources", "src/test/resources")
                        }
                    }
                }
            }

            val copyTaskName = "copyLtsFiles"
            // 2. Create a Copy task
            tasks.register<Copy>(copyTaskName) {
                from("../${ltsProjectName}/src") {
                    exclude("**/module-info.java")
                }

                destinationDir = File("${project.projectDir}/src-lts")
                // Ensure destination directory is empty before copying
                doFirst {
                    if (destinationDir.exists()) {
                        destinationDir.deleteRecursively()
                    }
                    destinationDir.mkdirs()
                }
            }

            // 3. Bind the copy task
            tasks.compileJava {
                dependsOn(copyTaskName)
            }

            tasks.named("sourcesJar") {
                dependsOn(copyTaskName)
            }

            tasks.named("processResources") {
                dependsOn(copyTaskName)
            }

            tasks.named("processTestResources") {
                dependsOn(copyTaskName)
            }
        }
    }
}


/*
 * Maven Publishing (local & central)
 */
val publicPublicationName = "sonatype"
val localPublicationName = "maven"

fun isMavenProject(arg: Project) : Boolean {
    return arg == arg.rootProject || arg.hasProperty("maven-project")
}

allprojects {
    afterEvaluate {
        if (isMavenProject(project)) {
            plugins.apply("maven-publish")
            plugins.apply("signing")
            tasks.withType<GenerateModuleMetadata> {
                enabled = false
            }
            publishing {
                createPublication(true, project)
                createPublication(false, project)
            }

            signing {
                sign(publishing.publications.named { name == publicPublicationName })
            }
        }
    }
}

fun createPublication(local: Boolean, pr: Project) {
    val publicationName = if (local) localPublicationName else publicPublicationName

    val isRoot = pr == pr.rootProject
    pr.publishing {
        publications {
            create<MavenPublication>(publicationName) {
                repositories {
                    if(local) {
                        mavenLocal()
                    } else {
                        mavenCentral {
                            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                            credentials {
                                username = findProperty("mavenCentralUserName") as String? ?: ""
                                password = findProperty("mavenCentralPassword") as String? ?: ""
                            }
                        }
                    }
                }

                if (isRoot) {
                    pom {
                        name = pr.name
                        description = "Java Rule Engine"
                        url = "https://www.evrete.org"
                        licenses {
                            license {
                                name = "MIT License"
                                url = "https://www.opensource.org/licenses/mit-license"
                                distribution = "May be downloaded from the Maven repository"
                            }
                        }
                        developers {
                            developer {
                                name = "Andrey Bichkevski"
                                email = "andbi@adbv.net"
                            }
                        }
                        scm {
                            connection = "scm:git:https://github.com/evrete/evrete.git"
                            developerConnection = "scm:git:https://github.com/evrete/evrete.git"
                            url = "https://github.com/evrete/evrete"
                            tag = "HEAD"
                        }
                    }
                } else {
                    pom {
                        name = pr.name
                        withXml {
                            asNode().appendNode("parent").apply {
                                appendNode("groupId", group)
                                appendNode("artifactId", pr.rootProject.name)
                                appendNode("version", version)
                            }
                        }
                    }
                    // Publish jars from child projects only
                    from(pr.components["java"])
                }
            }
        }
    }
}

// `mvn install` alias
allprojects {
    afterEvaluate {
        if(isMavenProject(this)) {
            val innerUglyMavenTaskName = "publish" + localPublicationName.capitalized() + "PublicationToMavenLocal"
            tasks.register("mavenInstall") {
                dependsOn(innerUglyMavenTaskName)
            }
        }
    }
}
