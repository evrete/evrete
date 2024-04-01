dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

extra["maven-project"] = "true"

java {
    withSourcesJar()
    withJavadocJar()
}
