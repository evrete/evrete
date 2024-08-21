version = "4.0.2"

plugins {
    id("maven-publish")
    id("signing")
}

val mavenVersion = rootProject.version.toString()
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name = "evrete"
                description = "Java Rule Engine"
                version = mavenVersion
                groupId = "org.evrete"
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

if (hasProperty("mavenCentralUserName")) {
    allprojects {
        apply(plugin = "signing")
        apply(plugin = "maven-publish")
        signing {
            sign(publishing.publications)
        }
    }
}

// `mvn install` alias
allprojects {
    afterEvaluate {
            val innerUglyMavenTaskName = "publishToMavenLocal"
            tasks.register("mavenInstall") {
                dependsOn(innerUglyMavenTaskName)
            }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }

    tasks.withType<Test> {
        testLogging.showStandardStreams = true
        val loggingConfigFile = project.file("src/test/resources/logging.properties").absolutePath
        jvmArgs = listOf("-Djava.util.logging.config.file=$loggingConfigFile")
    }
}


