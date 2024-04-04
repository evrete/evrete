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
    api(project(":evrete-core"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
    dependsOn(":evrete-core:publishMavenJavaPublicationToMavenLocal")
    dependsOn(":evrete-core-8:publishMavenJavaPublicationToMavenLocal")
}
tasks.named("publishMavenJavaPublicationToMavenRepoRepository") {
    dependsOn(":evrete-core:publishMavenJavaPublicationToMavenRepoRepository")
    dependsOn(":evrete-core-8:publishMavenJavaPublicationToMavenRepoRepository")
}

