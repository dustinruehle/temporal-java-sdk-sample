plugins {
    java
    application
}

group = "io.temporal.samples"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val temporalVersion = "1.33.0"

dependencies {
    implementation("io.temporal:temporal-sdk:$temporalVersion")
    implementation("io.temporal:temporal-opentracing:$temporalVersion")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("io.temporal:temporal-testing:$temporalVersion")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.15.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("execute") {
    group = "application"
    description = "Run a main class: ./gradlew execute -PmainClass=<fully.qualified.ClassName>"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(project.findProperty("mainClass") as String? ?: "")
}
