package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project

class GroupPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.group = "org.metaborg"
  }
}
