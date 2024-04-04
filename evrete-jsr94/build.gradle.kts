plugins {
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://dist.wso2.org/maven2/")
    }
}

dependencies {
    api(project(":evrete-core-8"))
    api("jsr94:jsr94:1.1")

    testImplementation(project(":evrete-dsl-java-8"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.javadoc {
    options {
        source = "11"
        showFromPublic()
        memberLevel = JavadocMemberLevel.PUBLIC
        encoding = "UTF-8"
    }
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

