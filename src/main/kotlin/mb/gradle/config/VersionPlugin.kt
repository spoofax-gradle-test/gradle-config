package mb.gradle.config

import mb.gitonium.GitoniumExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class VersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.withPlugin("org.metaborg.gitonium") {
      project.configure<GitoniumExtension> {
        // NOTE: reserved for if future changes to gitonium's settings are required.
      }
    }
  }
}
