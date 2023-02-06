import Constants.BLANK

//import BuildDeps.KOTLIN_VERSION
//import DomainDeps.KOIN_VERSION

object AppDeps {
    private const val implementation = "implementation"
    private const val runtimeOnly = "runtimeOnly"
    private const val developmentOnly = "developmentOnly"
    private const val testImplementation = "testImplementation"
    private const val kapt = "kapt"
    private const val annotationProcessor = "annotationProcessor"
    private const val testAnnotationProcessor = "testAnnotationProcessor"

    @JvmStatic
    val appModules by lazy {
        mapOf(
            implementation to implementationDeps,
            runtimeOnly to runtimeOnlyDeps,
            developmentOnly to developmentOnlyDeps,
            testImplementation to testDeps,
            kapt to kaptDeps,
            annotationProcessor to annotationProcessorDeps,
            testAnnotationProcessor to testAnnotationProcessorDeps,
        )
    }

    @JvmStatic
    val implementationDeps by lazy {mapOf(
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8" to BLANK,
        "org.jetbrains.kotlin:kotlin-reflect" to BLANK,
        "io.projectreactor.kotlin:reactor-kotlin-extensions" to BLANK,
        "org.jetbrains.kotlinx:kotlinx-coroutines-reactor" to BLANK,
"org.jetbrains.kotlinx:kotlinx-serialization-json" to "kotlinx_serialization_json.version"]}"),
//        //    implementation("io.projectreactor.tools:blockhound" to "blockhound_version"]}")
//        //    testImplementation("io.projectreactor.tools:blockhound-junit-platform" to "blockhound_version"]}")
"com.fasterxml.jackson.module:jackson-module-kotlin" to BLANK,
"org.apache.commons:commons-lang3" to BLANK
"org.springframework.boot:spring-boot-starter-actuator" to BLANK
"org.springframework.boot:spring-boot-starter-mail" to BLANK
"org.springframework.boot:spring-boot-starter-thymeleaf" to BLANK
"com.mailslurp:mailslurp-client-kotlin" to "mailslurp-client-kotlin.version"]}")
"org.springframework.boot:spring-boot-starter-validation" to BLANK
"org.springframework.boot:spring-boot-starter-webflux" to BLANK
"org.springframework.boot:spring-boot-starter-data-r2dbc" to BLANK
"org.springframework.boot:spring-boot-starter-security" to BLANK
"org.springframework.security:spring-security-data" to BLANK
"io.jsonwebtoken:jjwt-impl" to "jsonwebtoken.version"]}")
"io.jsonwebtoken:jjwt-jackson" to "jsonwebtoken.version"]}")
"io.netty:netty-tcnative-boringssl-static" to "boring_ssl.version"]}",
//        //    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
        )
         }

    @JvmStatic
    val runtimeOnlyDeps by lazy {
//        //    runtimeOnly ("com.google.appengine:appengine:+")
//        runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
//        runtimeOnly("com.h2database:h2")
//        runtimeOnly("io.r2dbc:r2dbc-h2")
//        //    runtimeOnly("io.r2dbc:r2dbc-postgresql")
//        //    runtimeOnly("org.postgresql:postgresql")

        emptyMap<String, String>()
    }

    @JvmStatic
    val testDeps by lazy {
//        testImplementation("org.jetbrains.kotlin:kotlin-test")
//        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
//        testImplementation("io.projectreactor:reactor-test")
//        testImplementation("org.mockito.kotlin:mockito-kotlin" to "mockito_kotlin_version"]}")
//        // Spring Test dependencies
//        testImplementation("org.springframework.boot:spring-boot-starter-test") { exclude(module = "mockito-core") }
//        // Mocking
//        testImplementation("io.mockk:mockk" to "mockk.version"]}")
//        testImplementation("com.github.tomakehurst:wiremock-jre8" to "wiremock.version"]}")
//        testImplementation("com.ninja-squad:springmockk" to "springmockk.version"]}")
//        // testcontainer
//        //    testImplementation("org.testcontainers:junit-jupiter")
//        //    testImplementation("org.testcontainers:postgresql")
//        //    testImplementation("org.testcontainers:r2dbc")
//        //testImplementation("com.tngtech.archunit:archunit-junit5-api" to "archunit_junit5_version"]}")
//        //testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine" to "archunit_junit5_version"]}")
//        //    testImplementation( "org.springframework.cloud:spring-cloud-starter-contract-verifier")


        emptyMap<String, String>() }

    @JvmStatic
    val developmentOnlyDeps by lazy {
//        developmentOnly("org.springframework.boot:spring-boot-devtools")

        emptyMap<String, String>() }
    @JvmStatic
    val kaptDeps by lazy { emptyMap<String, String>() }

    @JvmStatic
    val annotationProcessorDeps by lazy {
//        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        emptyMap<String, String>() }

    @JvmStatic
    val testAnnotationProcessorDeps by lazy { emptyMap<String, String>() }

}