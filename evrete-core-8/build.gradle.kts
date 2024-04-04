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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

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


// 1. Define vars
val ltsProjectName = project.name.substringBeforeLast("-8")
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

tasks.clean {
    dependsOn(copyTaskName)
}

tasks.processResources {
    dependsOn(copyTaskName)
}

tasks.processTestResources {
    dependsOn(copyTaskName)
}

tasks.named("sourcesJar") {
    dependsOn(copyTaskName)
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
            // Add the core Javadoc artifact
            artifact(project(":evrete-core").tasks["moduleJavadocJar"])
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

// Declare signing dependencies
tasks.named("publishMavenJavaPublicationToMavenLocal") {
    mustRunAfter(":evrete-core:publishMavenJavaPublicationToMavenLocal")
}

tasks.named("publishMavenJavaPublicationToMavenRepoRepository") {
    mustRunAfter(":evrete-core:publishMavenJavaPublicationToMavenRepoRepository")
}

