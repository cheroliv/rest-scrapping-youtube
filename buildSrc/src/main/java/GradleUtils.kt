@file:Suppress(
    "unused",
    "MemberVisibilityCanBePrivate",
    "UnusedReceiverParameter",
)

import AppDeps.appModules
import Constants.BLANK
import org.gradle.api.Project
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
        appModules.forEach { module ->
            module.second.first.forEach { dep ->
                mapOf(dep.first to dep.second).entries.first().run {
                    dependencies.add(module.first, dependency(this))
//                    dependencies.module(dependency(this)) {
//                        dep.third?.forEach { excl ->
//                            excl.forEach { (group, name) ->
//                                if (name != null && group.isNotBlank() && name.isNotBlank())
//                                    exclude(mapOf(group to name))
//                                if (group.isNotBlank())
//                                    exclude(mapOf(group to BLANK))
//                                if (!name.isNullOrBlank())
//                                    exclude(mapOf(BLANK to name))
//                            }
//                        }
//                    }
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
//        ),
//        setOf(
//            "org.junit.vintage" to "junit-vintage-engine",
//            "org.springframework.boot" to "spring-boot-starter-tomcat",
//            "org.apache.tomcat" to null
//        )
//    )
//}
