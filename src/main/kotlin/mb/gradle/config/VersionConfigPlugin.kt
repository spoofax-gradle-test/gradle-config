package mb.gradle.config

import mb.gitonium.GitoniumPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class VersionConfigPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(GitoniumPlugin::class)
  }
}
