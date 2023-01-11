@file:Suppress(
    "GradlePackageUpdate",
    "DEPRECATION",
)


import org.gradle.api.JavaVersion.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${properties["kotlin.version"]}")
    }
}

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.serialization")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.cloud.tools.jib")
    id("com.github.andygoossens.gradle-modernizer-plugin")
    id("com.google.cloud.tools.appengine")
    jacoco
}

group = properties["artifact.group"].toString()
version = properties["artifact.version"].toString()

//TODO: CLI apiclient to setup mailsurp
//create 2 inboxes: signup,password
tasks.register<DefaultTask>("addMailSlurpConfiguration") {
    group = "application"
    description = "add a yaml spring configuration for mailSlurp properties, and add the file to .gitignore"
    doFirst { println("addMailSlurpConfiguration") }
    //TODO: addMailSlurpConfiguration task
//check if src/main/resources/application-mailslurp.yml exists?
//when src/main/resources/application-mailslurp.yml dont exists then create file
//check if .gitignore exists?
//when .gitignore dont exists then create file
// and add src/main/resources/application-mailslurp.yml into .gitignore
//when .gitgnore exists then check if src/main/resources/application-mailslurp.yml is found into .gitignore
//when src/main/resources/application-mailslurp.yml is not found into .gitignore
// then add src/main/resources/application-mailslurp.yml to .gitgnore
}

//springBoot.mainClass.set("webapp.BackendBootstrap")
///*
//./gradlew -q cli --args='your args there'
// */
//tasks.register("cli") {
//    group = "application"
//    description = "Run webapp cli"
//    doFirst { springBoot.mainClass.set("webapp.CliBootstrap") }
//    finalizedBy("bootRun")
//}

repositories {
    google()
    mavenCentral()
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

dependencies {
//    implementation(project(path = ":common"))
    //Kotlin lib: jdk8, reflexion, coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${properties["kotlinx_serialization_json.version"]}")
    // Kotlin Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito_kotlin_version"]}")
    // Spring Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test") { exclude(module = "mockito-core") }
    // Mocking
    testImplementation("io.mockk:mockk:${properties["mockk.version"]}")
    testImplementation("com.github.tomakehurst:wiremock-jre8:${properties["wiremock.version"]}")
    testImplementation("com.ninja-squad:springmockk:${properties["springmockk.version"]}")
    // testcontainer
//    testImplementation("org.testcontainers:junit-jupiter")
//    testImplementation("org.testcontainers:postgresql")
//    testImplementation("org.testcontainers:r2dbc")
    //testImplementation("com.tngtech.archunit:archunit-junit5-api:${properties["archunit_junit5_version"]}")
    //testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${properties["archunit_junit5_version"]}")
//    testImplementation( "org.springframework.cloud:spring-cloud-starter-contract-verifier")
    //blockhound
//    implementation("io.projectreactor.tools:blockhound:${properties["blockhound_version"]}")
//    testImplementation("io.projectreactor.tools:blockhound-junit-platform:${properties["blockhound_version"]}")
    //jackson mapping (json/xml)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    //strings manipulation
    implementation("org.apache.commons:commons-lang3")
    //spring conf
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    //spring dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    //spring actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    //Spring mail
    implementation("org.springframework.boot:spring-boot-starter-mail")
    //spring thymeleaf for mail templating
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    //MailSlurp
    implementation("com.mailslurp:mailslurp-client-kotlin:${properties["mailslurp-client-kotlin.version"]}")
    //Spring bean validation JSR 303
    implementation("org.springframework.boot:spring-boot-starter-validation")
    //spring webflux reactive http
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    //spring r2dbc
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    //H2database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    //Postgresql
//    runtimeOnly("io.r2dbc:r2dbc-postgresql")
//    runtimeOnly("org.postgresql:postgresql")
    //Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-data")
    testImplementation("org.springframework.security:spring-security-test")
    // JWT authentication
    implementation("io.jsonwebtoken:jjwt-impl:${properties["jsonwebtoken.version"]}")
    implementation("io.jsonwebtoken:jjwt-jackson:${properties["jsonwebtoken.version"]}")
    //SSL
    implementation("io.netty:netty-tcnative-boringssl-static:${properties["boring_ssl.version"]}")
    //Spring Cloud
//    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
//    runtimeOnly ("com.google.appengine:appengine:+")
    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
    implementation.configure {
        setOf(
            "org.junit.vintage" to "junit-vintage-engine",
            "org.springframework.boot" to "spring-boot-starter-tomcat",
            "org.apache.tomcat" to null
        ).forEach { exclude(it.first, it.second) }
    }
}

java.sourceCompatibility = VERSION_1_8
java.targetCompatibility = VERSION_19

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(properties["free_compiler_args_value"].toString())
        jvmTarget = VERSION_17.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events(FAILED, SKIPPED) }
    reports {
        html.isEnabled = true
        ignoreFailures = true
    }
}

modernizer {
    failOnViolations = true
    includeTestClasses = true
}

tasks.register<Delete>("cleanResources") {
    description = "Delete directory build/resources"
    group = "build"
    delete("build/resources")
}

tasks.register<TestReport>("testReport") {
    description = "Generates an HTML test report from the results of testReport task."
    group = "report"
    destinationDir = file("$buildDir/reports/tests")
    reportOn("test")
}

jib {
    from {
        image = "eclipse-temurin:19.0.1_10-jre-alpine"
        platforms {
            platform {
                architecture = "${findProperty("jibArchitecture") ?: "amd64"}"
                os = "linux"
            }
        }
    }

    to {
        image = "cheroliv/kotlin-springboot"
        auth {
            username = properties["docker_hub_login"].toString()
            password = properties["docker_hub_login_token"].toString()
        }
    }
}