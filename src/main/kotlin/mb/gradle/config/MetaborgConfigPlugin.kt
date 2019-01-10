package mb.gradle.config

import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

open class MetaborgConfigExtension(private val project: Project) {
  fun configureSubProject() {
    project.configureGroup()
    project.configurePublishingRepositories()
  }


  fun configureJavaLibrary() {
    project.pluginManager.apply("java-library")
    project.configureJavaVersion()
    project.configureJavaPublication("JavaLibrary")
  }

  fun configureJavaApplication() {
    project.pluginManager.apply("application")
    project.configureJavaVersion()
    // TODO: this does not publish a runnable JAR?
    project.configureJavaPublication("JavaApplication")
  }

  fun configureKotlinLibrary() {
    project.pluginManager.apply("org.jetbrains.kotlin.jvm")
    project.configureKotlinVersion()
    project.configureKotlinStdLib()
    project.configureJavaPublication("KotlinLibrary")
  }

  fun configureKotlinGradlePlugin() {
    project.pluginManager.apply("kotlin-dsl")
    project.pluginManager.apply("maven-publish")
  }
}

class MetaborgConfigPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      configureGroup()
      // Only root project needs to configure version, as gitonium will set versions for subprojects automatically.
      configureVersion()
      configurePublishingRepositories()

      // Only root project needs composite build tasks, as these tasks depend on tasks for subprojects.
      tasks {
        createCompositeBuildTask(project, "cleanAll", "clean", "Deletes the build directory for all projects in the composite build.")
        createCompositeBuildTask(project, "checkAll", "check", "Runs all checks for all projects in the composite build.")
        createCompositeBuildTask(project, "assembleAll", "assemble", "Assembles the outputs for all projects in the composite build.")
        createCompositeBuildTask(project, "buildAll", "build", "Assembles and tests all projects in the composite build.")
        createCompositeBuildTask(project, "publishAll", "publish", "Publishes all publications produced by all projects in the composite build.")
      }

      extensions.add("metaborg-config", MetaborgConfigExtension(project))
      subprojects {
        extensions.add("metaborg-config", MetaborgConfigExtension(project))
      }
    }
  }

  private fun TaskContainerScope.createCompositeBuildTask(project: Project, allName: String, name: String, description: String) {
    register(allName) {
      this.group = "composite build"
      this.description = description
      if(project.subprojects.isEmpty()) {
        val task = project.tasks.findByName(name)
        if(task != null) {
          this.dependsOn(task)
        } else {
          project.logger.warn("Composite build task '$allName' does not include project '$project' because it does not have a task named '$name'")
        }
      } else {
        this.dependsOn(project.subprojects.mapNotNull {
          it.tasks.findByName(name) ?: run {
            project.logger.warn("Composite build task '$allName' does not include project '$it' because it does not have a task named '$name'")
            null
          }
        })
      }
    }
  }
}


private fun Project.configureGroup() {
  group = "org.metaborg"
}

private fun Project.configureVersion() {
  pluginManager.apply("org.metaborg.gitonium")
}

private fun Project.configurePublishingRepositories() {
  pluginManager.apply("publishing")
  configure<PublishingExtension> {
    repositories {
      maven {
        name = "Artifactory"
        url = URI("http://home.gohla.nl:8091/artifactory/all/")
        credentials {
          username = project.findProperty("publish.repository.Artifactory.username")?.toString()
          password = project.findProperty("publish.repository.Artifactory.password")?.toString()
        }
      }
    }
  }
}


private fun Project.configureJavaVersion() {
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

private fun Project.configureJavaPublication(name: String) {
  pluginManager.apply("maven-publish")
  configure<PublishingExtension> {
    publications {
      create<MavenPublication>(name) {
        from(project.components["java"])
      }
    }
  }
}


private fun Project.configureKotlinVersion() {
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
  }
}

private fun Project.configureKotlinStdLib() {
  val compile by configurations
  dependencies {
    compile(kotlin("stdlib"))
  }
}