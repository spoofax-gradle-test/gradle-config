package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project

class GroupConfigPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.group = "org.metaborg"
  }
}
