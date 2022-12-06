pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
    plugins {
        kotlin("jvm").version(extra["kotlin.version"].toString())
        kotlin("plugin.serialization").version(extra["kotlin.version"].toString())
        kotlin("plugin.allopen").version(extra["kotlin.version"].toString())
        kotlin("plugin.noarg").version(extra["kotlin.version"].toString())
        kotlin("plugin.spring").version(extra["kotlin.version"].toString())
        kotlin("multiplatform").version(extra["kotlin.version"].toString())
        id("org.jetbrains.compose").version(extra["compose.version"].toString())
        id("org.springframework.boot").version(extra["springboot.version"].toString())
        id("io.spring.dependency-management").version(extra["spring_dependency_management.version"].toString())
        id("com.google.cloud.tools.jib").version(extra["jib.version"].toString())
        id("com.github.andygoossens.gradle-modernizer-plugin").version(extra["modernizer.version"].toString())
    }
}

rootProject.name = "kotlin-springboot-monolith"
include(":monolith")
include(":desktop")