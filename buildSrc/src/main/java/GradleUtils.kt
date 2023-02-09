@file:Suppress(
    "unused",
    "MemberVisibilityCanBePrivate",
    "UnusedReceiverParameter",
)

import AppDeps.appModules
import Constants.BLANK
import org.gradle.api.Project
import org.gradle.kotlin.dsl.module
import java.lang.System.getProperty

object GradleUtils {
    /*=================================================================================*/
    val Project.sep: String get() = getProperty("file.separator")

    /*=================================================================================*/
    fun Project.dependency(entry: Map.Entry<String, String?>) = entry.run {
        key + when (value) {
            null -> BLANK
            BLANK -> BLANK
            else -> ":${properties[value]}"
        }
    }

    /*=================================================================================*/
//    fun Project.appDependencies() {
//        appModules.forEach { module ->
//            module.value.first.forEach {
//                dependencies.add(module.key, dependency(it))
//            }
//        }
//    }

    fun Project.appDependencies() {
        appModules.forEach { module: Map.Entry<String, Pair<Set<Triple<String, String, Set<Map<String, String?>>?>>, Set<Pair<String, String?>>?>> ->
            module.value.first.forEach { dep: Triple<String, String, Set<Map<String, String?>>?> ->
                mapOf(dep.first to dep.second).entries.first().run {
                    dependencies.add(module.key, dependency(this))
                    dependencies.module(dependency(this)) {
                        dep.third?.forEach { excl->
                            excl.forEach { t: String, u: String? ->

                            }
                        }
                    }
//                        dependency(this))


                }
            }
        }
    }
}
/*=================================================================================*/
//@JvmStatic
//val implementationDeps by lazy {
//    Pair(
//        setOf(
//            Triple("org.jetbrains.kotlin:kotlin-stdlib-jdk8", BLANK, null),
//            Triple("org.jetbrains.kotlin:kotlin-reflect", BLANK, null),
//            Triple("org.jetbrains.kotlin:kotlin-stdlib-jdk8", BLANK, null),
//            Triple("org.jetbrains.kotlin:kotlin-reflect", BLANK, null),
//            Triple("io.projectreactor.kotlin:reactor-kotlin-extensions", BLANK, null),
//            Triple("org.jetbrains.kotlinx:kotlinx-coroutines-reactor", BLANK, null),
//            Triple("org.jetbrains.kotlinx:kotlinx-serialization-json", "kotlinx_serialization_json.version", null),
//            Triple("com.fasterxml.jackson.module:jackson-module-kotlin", BLANK, null),
//            Triple("org.apache.commons:commons-lang3", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-actuator", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-mail", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-thymeleaf", BLANK, null),
//            Triple("com.mailslurp:mailslurp-client-kotlin", "mailslurp-client-kotlin.version", null),
//            Triple("org.springframework.boot:spring-boot-starter-validation", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-webflux", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-data-r2dbc", BLANK, null),
//            Triple("org.springframework.boot:spring-boot-starter-security", BLANK, null),
//            Triple("org.springframework.security:spring-security-data", BLANK, null),
//            Triple("io.jsonwebtoken:jjwt-impl", "jsonwebtoken.version", null),
//            Triple("io.jsonwebtoken:jjwt-jackson", "jsonwebtoken.version", null),
//            Triple("io.netty:netty-tcnative-boringssl-static", "boring_ssl.version", null),
//            //    implementation("org.springframework.cloud:spring-cloud-gcp-starter-storage")
//            //    implementation("io.projectreactor.tools:blockhound" to "blockhound_version",
//        ),
//        setOf(
//            "org.junit.vintage" to "junit-vintage-engine",
//            "org.springframework.boot" to "spring-boot-starter-tomcat",
//            "org.apache.tomcat" to null
//        )
//    )
//}
