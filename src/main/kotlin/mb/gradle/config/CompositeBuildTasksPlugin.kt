package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class CompositeBuildTasksPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks {
      createCompositeBuildTask(project, "cleanAll", "clean", "Deletes the build directory for all projects in the composite build.")
      createCompositeBuildTask(project, "checkAll", "check", "Runs all checks for all projects in the composite build.")
      createCompositeBuildTask(project, "assembleAll", "assemble", "Assembles the outputs for all projects in the composite build.")
      createCompositeBuildTask(project, "buildAll", "build", "Assembles and tests all projects in the composite build.")
      createCompositeBuildTask(project, "publishAll", "publish", "Publishes all publications produced by all projects in the composite build.")
    }
  }

  private fun TaskContainerScope.createCompositeBuildTask(project: Project, allName: String, name: String, description: String) {
    register(allName) {
      this.group = "composite build"
      this.description = description
      if(project.subprojects.isEmpty()) {
        this.dependsOn(name)
      } else {
        this.dependsOn(project.subprojects.map { it.tasks[name] })
      }
    }
  }
}
