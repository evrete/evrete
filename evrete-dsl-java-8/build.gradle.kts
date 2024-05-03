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
    api(project(":evrete-core-8"))
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
val deleteTaskName = "deleteLtsFiles"

val ltsDir = File("${project.projectDir}/src-lts")

// 2. Create a delete task
tasks.register<Delete>(deleteTaskName) {
    delete(ltsDir)

    doLast {
        ltsDir.mkdirs()
    }
}

// 3. Create a Copy task
tasks.register<Copy>(copyTaskName) {
    destinationDir = ltsDir
    from("../${ltsProjectName}/src") {
        exclude("**/module-info.java")
    }
}

// 4. Bind the tasks
tasks.getByName(copyTaskName).dependsOn(deleteTaskName)

tasks.compileJava {
    dependsOn(copyTaskName)
}

tasks.clean {
    dependsOn(deleteTaskName)
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
    mustRunAfter(":evrete-core-8:publishMavenJavaPublicationToMavenLocal")
    mustRunAfter(":evrete-dsl-java:publishMavenJavaPublicationToMavenLocal")
}

tasks.named("publishMavenJavaPublicationToMavenRepoRepository") {
    mustRunAfter(":evrete-core:publishMavenJavaPublicationToMavenRepoRepository")
    mustRunAfter(":evrete-core-8:publishMavenJavaPublicationToMavenRepoRepository")
    mustRunAfter(":evrete-dsl-java:publishMavenJavaPublicationToMavenRepoRepository")
}
