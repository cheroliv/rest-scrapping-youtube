@file:Suppress(
    "GradlePackageUpdate",
    "DEPRECATION",
)

import org.gradle.api.JavaVersion.VERSION_18
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.io.ByteArrayOutputStream

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

dependencies {
//    implementation(project(path = ":common"))
    //Kotlin lib: jdk8, reflexion, coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${properties["kotlinx_serialization_json.version"]}")
    // kotlin TDD
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito_kotlin_version"]}")
    //blockhound
//    implementation("io.projectreactor.tools:blockhound:${properties["blockhound_version"]}")
//    testImplementation("io.projectreactor.tools:blockhound-junit-platform:${properties["blockhound_version"]}")

    //jackson mapping (json/xml)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    //strings manipulation
    implementation("org.apache.commons:commons-lang3")
    //Http Request Exception to Problem Response
    implementation("org.zalando:problem-spring-webflux:${properties["zalando_problem.version"]}")
    //spring conf
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    //spring dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    //spring actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    //spring r2dbc
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    //spring javax.mail
    implementation("org.springframework.boot:spring-boot-starter-mail")
    //to get mail constants
    implementation("org.apache.commons:commons-email:${properties["commons_email.version"]}") {
        setOf(
            "junit",
            "org.easymock",
            "org.powermock",
            "org.slf4j ",
            "commons-io",
            "org.subethamail",
            "com.sun.mail"
        ).map { exclude(it) }
    }
    //Spring bean validation JSR 303
    implementation("org.springframework.boot:spring-boot-starter-validation")
    //spring thymeleaf for mail templating
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    //spring webflux reactive http
    implementation("org.springframework.boot:spring-boot-starter-webflux")
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
    // spring Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test") { exclude(module = "mockito-core") }
    // Mocking
    testImplementation("io.mockk:mockk:${properties["mockk.version"]}")
    testImplementation("com.github.tomakehurst:wiremock-jre8:${properties["wiremock.version"]}")
    testImplementation("com.ninja-squad:springmockk:3.1.0")
    // BDD - Cucumber
    testImplementation("io.cucumber:cucumber-java8:${properties["cucumber_java.version"]}")
    testImplementation("io.cucumber:cucumber-java:${properties["cucumber_java.version"]}")


    // testcontainer
//    testImplementation("org.testcontainers:junit-jupiter")
//    testImplementation("org.testcontainers:postgresql")
//    testImplementation("org.testcontainers:r2dbc")
    //testImplementation("com.tngtech.archunit:archunit-junit5-api:${properties["archunit_junit5_version"]}")
    //testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${properties["archunit_junit5_version"]}")


//    testImplementation( "org.springframework.cloud:spring-cloud-starter-contract-verifier")
//    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
//    providedCompile ("com.google.appengine:appengine:+")

    implementation("com.mailslurp:mailslurp-client-kotlin:15.14.0")


}

sourceSets {
    getByName("main") {
        java.srcDir("private/resources")
    }
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
    implementation.configure {
        setOf(
            "org.junit.vintage" to "junit-vintage-engine",
            "org.springframework.boot" to "spring-boot-starter-tomcat",
            "org.apache.tomcat" to null
        ).map { exclude(it.first, it.second) }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(properties["free_compiler_args_value"].toString())
        jvmTarget = VERSION_18.toString()
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
val cucumberRuntime: Configuration by configurations.creating {
    extendsFrom(configurations["testImplementation"])
}

tasks.register<DefaultTask>("cucumber") {
    group = "verification"
    dependsOn("assemble", "compileTestJava")
    doLast {
        javaexec {
            mainClass.set("io.cucumber.core.cli.Main")
            classpath = cucumberRuntime + sourceSets.main.get().output + sourceSets.test.get().output
            // Change glue for your project package where the step definitions are.
            // And where the feature files are.
            args = listOf(
                "--plugin",
                "pretty",
                "--glue",
                "features",
                "src/test/resources/features"
            )
            // Configure jacoco agent for the test coverage in the string interpolation.
            jvmArgs = listOf(
                "-javaagent:${
                    zipTree(
                        configurations
                            .jacocoAgent
                            .get()
                            .singleFile
                    ).filter { it.name == "jacocoagent.jar" }.singleFile
                }=destfile=$buildDir/results/jacoco/cucumber.exec,append=false"
            )
        }
    }
}

tasks.jacocoTestReport {
    // Give jacoco the file generated with the cucumber tests for the coverage.
    executionData(
        files(
            "$buildDir/jacoco/test.exec",
            "$buildDir/results/jacoco/cucumber.exec"
        )
    )
    reports {
        xml.required.set(true)
    }
}

open class DeployGAE : Exec() {
    init {
        workingDir = project.rootDir
        this.commandLine(
            "/snap/bin/gcloud",
            "-v"
//            "app",
//            "deploy",
//            "${projectDir.absolutePath}/src/main/appengine/app.yml"
        )
        standardOutput = ByteArrayOutputStream()
    }
}


tasks.register<DeployGAE>("deployGAE") {
    group = "application"
    val cmd = "gcloud app deploy src/main/appengine/app.flexible.yml"
    doLast { println(cmd) }
}

//springBoot.mainClass.set("backend.BackendBootstrap")
///*
//./gradlew -q cli --args='your args there'
// */
//tasks.register("cli") {
//    group = "application"
//    description = "Run backend cli"
//    doFirst { springBoot.mainClass.set("backend.CliBootstrap") }
//    finalizedBy("bootRun")
//}


