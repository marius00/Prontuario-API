plugins {
    kotlin("jvm") version "2.1.21" // Spring support expected in 2025-11 for Kotlin 2.2
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.netflix.dgs.codegen") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

group = "prontuario.al"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext["junit-jupiter.version"] = "5.13.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.netflix.graphql.dgs:dgs-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.ktorm:ktorm-core:4.1.1")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.mysql:mysql-connector-j:9.4.0")
    implementation("com.netflix.graphql.dgs:graphql-dgs-extended-validation")
    implementation("org.flywaydb:flyway-core:11.11.2")
    implementation("org.flywaydb:flyway-mysql:11.11.2")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Needed for testing the HTTP layer using dgs-starter-test
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.netflix.graphql.dgs:dgs-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Logging: Should probably find something better, seems to have a volatile API
    implementation("org.springframework.boot:spring-boot-starter-aop:3.4.5")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("com.github.ksuid:ksuid:1.1.3")
    implementation("commons-io:commons-io:2.19.0")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("commons-validator:commons-validator:1.9.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:10.1.2")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/schema")
    packageName = "prontuario.al.generated"
    generateClient = true

    typeMapping = mapOf(
        "RoleEnum" to "prontuario.al.auth.RoleEnum",
        "LevelEnum" to "prontuario.al.auth.LevelEnum",
    ).toMutableMap()
}
tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    manifest {
        attributes["mainClassName"] = "prontuario.al.Application"
    }
}
