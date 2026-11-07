import net.ltgt.gradle.errorprone.errorprone

// The GitHub sign-in bridge (ADR 20): a small OpenID Connect provider in front of GitHub's
// OAuth, which Entra External ID federates. Included from api/settings.gradle.kts, so the plugins
// are already on the classpath from the API's build and are applied here without versions.
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

group = "io.github.aaronfilson"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Downstream: the OpenID Connect provider that Entra federates
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server")
    // Upstream: signing in with GitHub's OAuth
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    // A rate limit on /oauth2/authorize, in memory (plan 18)
    implementation(libs.bucket4j)
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
    archiveFileName = "studiomanager-auth.jar"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    // The same bar as the API: every lint and Error Prone warning fails the build
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-Werror"))
    options.errorprone {
        error("NullAway")
        disable("MissingSummary")
        option("NullAway:JSpecifyMode", "true")
        option("NullAway:OnlyNullMarked", "true")
    }
}

tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.enabled = false
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get())
        removeUnusedImports()
    }
}
