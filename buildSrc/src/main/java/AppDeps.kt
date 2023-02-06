import Constants.BLANK

//import BuildDeps.KOTLIN_VERSION
//import DomainDeps.KOIN_VERSION

object AppDeps {
    private const val implementation = "implementation"
    private const val runtimeOnly = "runtimeOnly"
    private const val developmentOnly = "developmentOnly"
    private const val testImplementation = "testImplementation"
    private const val testRuntimeOnly = "testRuntimeOnly"
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
            testRuntimeOnly to testRuntimeOnlyDeps,
            kapt to kaptDeps,
            annotationProcessor to annotationProcessorDeps,
            testAnnotationProcessor to testAnnotationProcessorDeps,
        )
    }

    @JvmStatic
    val implementationDeps = Pair(
        mapOf(
//            Pair("org.jetbrains.kotlin:kotlin-stdlib-jdk8" to BLANK, emptyMap<String, String?>()),
//            Pair("org.jetbrains.kotlin:kotlin-reflect" to BLANK, emptyMap<String, String?>()),
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8" to BLANK,
            "org.jetbrains.kotlin:kotlin-reflect" to BLANK,
            "io.projectreactor.kotlin:reactor-kotlin-extensions" to BLANK,
            "org.jetbrains.kotlinx:kotlinx-coroutines-reactor" to BLANK,
            "org.jetbrains.kotlinx:kotlinx-serialization-json" to "kotlinx_serialization_json.version",
            "com.fasterxml.jackson.module:jackson-module-kotlin" to BLANK,
            "org.apache.commons:commons-lang3" to BLANK,
            "org.springframework.boot:spring-boot-starter-actuator" to BLANK,
            "org.springframework.boot:spring-boot-starter-mail" to BLANK,
            "org.springframework.boot:spring-boot-starter-thymeleaf" to BLANK,
            "com.mailslurp:mailslurp-client-kotlin" to "mailslurp-client-kotlin.version",
            "org.springframework.boot:spring-boot-starter-validation" to BLANK,
            "org.springframework.boot:spring-boot-starter-webflux" to BLANK,
            "org.springframework.boot:spring-boot-starter-data-r2dbc" to BLANK,
            "org.springframework.boot:spring-boot-starter-security" to BLANK,
            "org.springframework.security:spring-security-data" to BLANK,
            "io.jsonwebtoken:jjwt-impl" to "jsonwebtoken.version",
            "io.jsonwebtoken:jjwt-jackson" to "jsonwebtoken.version",
            "io.netty:netty-tcnative-boringssl-static" to "boring_ssl.version",
            //    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
            //    implementation("io.projectreactor.tools:blockhound" to "blockhound_version",
        ),
        setOf(
            "org.junit.vintage" to "junit-vintage-engine",
            "org.springframework.boot" to "spring-boot-starter-tomcat",
            "org.apache.tomcat" to null
        )
    )


    @JvmStatic
    val runtimeOnlyDeps by lazy {
        Pair(
            mapOf(
                "org.springframework.boot:spring-boot-properties-migrator" to BLANK,
                "com.h2database:h2" to BLANK,
                "io.r2dbc:r2dbc-h2" to BLANK,
//        //    runtimeOnly ("com.google.appengine:appengine:+")
//        //    runtimeOnly("io.r2dbc:r2dbc-postgresql")
//        //    runtimeOnly("org.postgresql:postgresql")
            ),
            emptySet<Pair<String, String?>>()
        )
    }


    @JvmStatic
    val testRuntimeOnlyDeps by lazy {
        //        //testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine" to "archunit_junit5_version",
        Pair(emptyMap<String, String>(), emptySet<Pair<String, String?>>())
    }

    @JvmStatic
    val testDeps by lazy {
        Pair(
            mapOf(
                "org.jetbrains.kotlin:kotlin-test" to BLANK,
                "org.jetbrains.kotlin:kotlin-test-junit5" to BLANK,
                "io.projectreactor:reactor-test" to BLANK,
                "org.mockito.kotlin:mockito-kotlin" to "mockito_kotlin_version",
                "org.springframework.boot:spring-boot-starter-test" to BLANK,
                "io.mockk:mockk" to "mockk.version",
                "com.github.tomakehurst:wiremock-jre8" to "wiremock.version",
                "com.ninja-squad:springmockk" to "springmockk.version",
//        //    testImplementation("io.projectreactor.tools:blockhound-junit-platform" to "blockhound_version",
                //    "org.testcontainers:junit-jupiter" to BLANK,
                //    "org.testcontainers:postgresql" to BLANK,
                //    "org.testcontainers:r2dbc" to BLANK,
                //    "com.tngtech.archunit:archunit-junit5-api" to "archunit_junit5_version",
                //     "org.springframework.cloud:spring-cloud-starter-contract-verifier" to BLANK,
//                Pair("org.springframework.boot:spring-boot-starter-test" to BLANK, "mockito-core")
            ),
            emptySet<Pair<String, String?>>()
        )
    }

    @JvmStatic
    val developmentOnlyDeps by lazy {
        Pair(
            mapOf("org.springframework.boot:spring-boot-devtools" to BLANK),
            emptySet<Pair<String, String?>>()
        )
    }

    @JvmStatic
    val kaptDeps by lazy {
        Pair(emptyMap<String, String>(), emptySet<Pair<String, String?>>())
    }

    @JvmStatic
    val annotationProcessorDeps by lazy {
        Pair(
            mapOf("org.springframework.boot:spring-boot-configuration-processor" to BLANK),
            emptySet<Pair<String, String?>>()
        )
    }

    @JvmStatic
    val testAnnotationProcessorDeps by lazy {
        Pair(
            emptyMap<String, String>(),
            emptySet<Pair<String, String?>>()
        )
    }
}