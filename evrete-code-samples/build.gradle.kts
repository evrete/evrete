
plugins {
    id("java-library")
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
