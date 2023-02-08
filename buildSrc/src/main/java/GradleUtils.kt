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
    fun Project.appDependencies() {
        appModules.forEach { module ->
            module.value.first.forEach {
                dependencies.add(module.key, dependency(it))
            }
        }
    }
}
/*=================================================================================*/

