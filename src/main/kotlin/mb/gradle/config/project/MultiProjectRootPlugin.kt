package mb.gradle.config.project

import mb.gradle.config.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class MultiProjectRootPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(GroupPlugin::class)
    project.pluginManager.apply(VersionPlugin::class)
    project.pluginManager.apply(CompositeBuildTasksPlugin::class)
  }
}
