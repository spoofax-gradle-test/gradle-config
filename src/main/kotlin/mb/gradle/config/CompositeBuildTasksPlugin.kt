package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class CompositeBuildTasksPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks {
      createCompositeBuildTask(project, "cleanAll", "clean")
      createCompositeBuildTask(project, "checkAll", "check")
      createCompositeBuildTask(project, "assembleAll", "assemble")
      createCompositeBuildTask(project, "buildAll", "build")
      createCompositeBuildTask(project, "publishAll", "publish")
    }
  }

  private fun TaskContainerScope.createCompositeBuildTask(project: Project, allName: String, name: String) {
    register(allName) {
      group = "composite build"
      if(project.subprojects.isEmpty()) {
        dependsOn(name)
      } else {
        dependsOn(project.subprojects.map { it.tasks[name] })
      }
    }
  }
}
