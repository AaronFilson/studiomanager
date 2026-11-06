import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
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
    // Null checking at compile time: Error Prone hosts NullAway (plan 20, section 2)
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    // The event publication registry, kept in Postgres: the transactional outbox for email
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    // Email through Azure Communication Services, authenticated by the managed identity
    implementation(libs.azure.communication.email)
    implementation(libs.azure.identity)
    // Piece photos: the API signs upload and read URLs; the bytes go straight to Blob Storage
    implementation(libs.azure.storage.blob)
    // Outbound webhooks: its DnsResolver hook lets the SSRF filter connect only to the addresses it
    // checked, which the JDK HttpClient can't do. The version comes from Spring Boot's BOM.
    implementation("org.apache.httpcomponents.client5:httpclient5")
    // Reading invoices with the studio's own Claude key (docs/planning/16-invoice-ocr-plan.md)
    implementation(libs.anthropic)
    // Event streaming (off unless studiomanager.stream.enabled): Modulith publishes the stream
    // contract to Kafka through the same outbox as email (ADR 15, docs/adr/0015-event-streaming-to-kafka.md)
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.modulith:spring-modulith-events-kafka")
    // OpenTelemetry traces, metrics and logs to Application Insights. Off unless the azure profile
    // turns it on; the connection string comes from APPLICATIONINSIGHTS_CONNECTION_STRING
    implementation(libs.azure.monitor)
    runtimeOnly("org.postgresql:postgresql")
    // Passwordless Postgres on Azure: the JDBC driver gets an Entra token from the managed identity
    runtimeOnly(libs.azure.identity.extensions)
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-runtime")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
        // Keeps the OpenTelemetry SDK in step with the instrumentation under
        // spring-cloud-azure-starter-monitor. Left to Spring Boot's BOM, the SDK can move ahead of
        // the starter, and the context fails to start. TelemetryTests catches that, so check it
        // when bumping this.
        mavenBom(libs.opentelemetry.instrumentation.bom.get().toString())
    }
}

// OpenAPI-first: the spec is the source of truth. Controllers implement these generated
// interfaces, so a spec change that the server hasn't caught up with fails compilation.
openApiGenerate {
    generatorName = "spring"
    inputSpec = layout.projectDirectory.file("src/main/openapi/studiomanager.yaml")
    outputDir = layout.buildDirectory.dir("generated/openapi")
    apiPackage = "io.github.aaronfilson.studiomanager.contract"
    modelPackage = "io.github.aaronfilson.studiomanager.contract"
    modelNameSuffix = "Dto"
    globalProperties = mapOf("apis" to "", "models" to "", "modelDocs" to "false", "modelTests" to "false")
    configOptions = mapOf(
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "useTags" to "true",
        "useSpringBoot4" to "true",
        "useJackson3" to "true",
        "useJspecify" to "true",
        "openApiNullable" to "false",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "hideGenerationTimestamp" to "true",
        "generateJsonIncludeAnnotations" to "false",
        "generateJsonSetterNullsAnnotations" to "false",
    )
}

sourceSets {
    main {
        java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.bootJar {
    // A stable name keeps the Dockerfile independent of the version
    archiveFileName = "studiomanager-api.jar"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    // Warnings for now; -Werror comes at the end of plan 20
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial"))
    options.errorprone {
        // Only NullAway for now. Error Prone's own checks are sorted out at the end of plan 20.
        disableAllChecks = true
        error("NullAway")
        disableWarningsInGeneratedCode = true
        excludedPaths = ".*/build/generated/.*"
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
