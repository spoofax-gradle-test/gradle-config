package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

class RepositoriesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.repositories {
      maven(url = "http://home.gohla.nl:8091/artifactory/all/")
    }
  }
}
