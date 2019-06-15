import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.3.21"
}

group = "seiderer"
version = "Alpha-0.1"

application {
    mainClassName = "de.seiderer.eventerpretor.main.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    compile(fileTree("libs") { include("*.jar") })
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClassName
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")

    from(configurations.runtime.get().map {if (it.isDirectory) it else zipTree(it)})
}