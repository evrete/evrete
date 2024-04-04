
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
    implementation(project(":evrete-dsl-java"))
}


tasks.register<JavaExec>("howtoCsvFactsInline") {
    mainClass = "org.evrete.examples.howto.CsvFactsInline"
    classpath = sourceSets.main.get().runtimeClasspath
}
