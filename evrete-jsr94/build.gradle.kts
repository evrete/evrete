repositories {
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


extra["maven-project"] = "true"

java {
    withSourcesJar()
    withJavadocJar()
}
