package mb.gradle.config.project

import mb.gradle.config.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class SubProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(GroupPlugin::class)
    project.pluginManager.apply(RepositoriesPlugin::class)
    project.pluginManager.apply(PublishingRepositoriesPlugin::class)
    project.pluginManager.apply(JavaPlugin::class)
    project.pluginManager.apply(KotlinPlugin::class)
  }
}
