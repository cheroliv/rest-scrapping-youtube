import AppDeps.appModules
import Constants.BLANK
import org.gradle.api.Project
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.exclude
import java.lang.System.getProperty

object GradleUtils {
    /*=================================================================================*/
    @JvmStatic
    val sep: String by lazy { getProperty("file.separator") }

    /*=================================================================================*/
    @JvmStatic
    fun Project.dependency(entry: Map.Entry<String, String?>) = entry.run {
        key + when (value) {
            null -> BLANK
            BLANK -> BLANK
            else -> ":${properties[value]}"
        }
    }

    /*=================================================================================*/
    @JvmStatic
    fun Project.appDependencies() {
        appModules.forEach { module ->
            module.value.first.forEach {
                dependencies.add(module.key, dependency(it))
            }
        }
    }
}
/*=================================================================================*/

