plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.26.0"
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

extra["springModulithVersion"] = "2.1.1"

dependencies {
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
    implementation("com.azure:azure-communication-email:1.1.6")
    implementation("com.azure:azure-identity:1.18.7")
    // Piece photos: the API signs upload and read URLs; the bytes go straight to Blob Storage
    implementation("com.azure:azure-storage-blob:12.35.2")
    runtimeOnly("org.postgresql:postgresql")
    // Passwordless Postgres on Azure: the JDBC driver gets an Entra token from the managed identity
    runtimeOnly("com.azure:azure-identity-extensions:1.2.10")
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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
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
