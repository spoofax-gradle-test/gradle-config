package mb.gradle.config

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

//
// Project plugins.
//

@Suppress("unused")
class RootProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureRootProject()
  }
}

@Suppress("unused")
class SubProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureSubProject()
  }
}

//
// Goal-based plugins.
//

@Suppress("unused")
class JavaLibraryPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJavaLibrary()
  }
}

@Suppress("unused")
class JavaApplicationPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJavaApplication()
  }
}

@Suppress("unused")
class KotlinLibraryPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureKotlinLibrary()
  }
}

//
// Extension.
//

@Suppress("unused")
open class MetaborgConfigExtension(private val project: Project) {
  companion object {
    const val name = "metaborgConfig"
  }

  fun configureSubProject() {
    project.configureSubProject()
  }

  fun configureJavaLibrary() {
    project.configureJavaLibrary()
  }

  fun configureJavaApplication() {
    project.configureJavaApplication()
  }

  fun configureKotlinLibrary() {
    project.configureKotlinLibrary()
  }
}

//
// (Sub-)project configuration.
//

private fun Project.configureAnyProject() {
  configureGroup()
  configureRepositories()
  configurePublishingRepositories()
  if(extensions.findByName(MetaborgConfigExtension.name) == null) {
    extensions.add(MetaborgConfigExtension.name, MetaborgConfigExtension(this))
  }
  subprojects {
    if(extensions.findByName(MetaborgConfigExtension.name) == null) {
      extensions.add(MetaborgConfigExtension.name, MetaborgConfigExtension(this))
    }
  }
}

private fun Project.configureRootProject() {
  configureAnyProject()
  createCompositeBuildTasks()
}

private fun Project.configureSubProject() {
  configureAnyProject()
  // Only root project needs composite build tasks, as these tasks depend on tasks for subprojects.
}

//
// Shared configuration.
//

private fun Project.configureGroup() {
  group = "org.metaborg"
}

private fun Project.configureRepositories() {
  repositories {
    maven(url = "http://home.gohla.nl:8091/artifactory/all/")
    jcenter() // Use JCenter as backup.
  }
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

private fun Project.createCompositeBuildTasks() {
  tasks {
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

//
// Java configuration.
//

private fun Project.configureJavaVersion() {
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

private fun Project.configureJavaPublication(name: String, additionalConfiguration: MavenPublication.() -> Unit = {}) {
  pluginManager.apply("maven-publish")
  configure<PublishingExtension> {
    publications {
      create<MavenPublication>(name) {
        from(project.components["java"])
        additionalConfiguration()
      }
    }
  }
}

private fun Project.configureJavaLibrary() {
  pluginManager.apply("java-library")
  configureJavaVersion()
  configureJavaPublication("JavaLibrary")
}

fun Project.configureJavaApplication() {
  pluginManager.apply("application")
  configureJavaVersion()
  afterEvaluate {
    // Create additional JAR task that creates an executable JAR.
    val jarTask = tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)
    val executableJarTask = tasks.register("executableJar", Jar::class) {
      manifest {
        attributes["Main-Class"] = project.the<JavaApplication>().mainClassName
      }
      archiveClassifier.set("executable")
      val runtimeClasspath by configurations
      from(runtimeClasspath.filter { it.exists() }.map { if(it.isDirectory) it else zipTree(it) })
      with(jarTask)
    }
    tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(executableJarTask)
    // Create an artifact for the executable JAR.
    val executableJarArtifact = artifacts.add("archives", executableJarTask) {
      classifier = "executable"
    }
    // Publish primary artifact from the Java component, and publish executable JAR and ZIP distribution as secondary artifacts.
    configureJavaPublication("JavaApplication") {
      artifact(executableJarArtifact)
      artifact(tasks.getByName("distZip"))
    }
  }
}

//
// Kotlin configuration.
//

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

fun Project.configureKotlinLibrary() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  configureKotlinVersion()
  configureKotlinStdLib()
  configureJavaPublication("KotlinLibrary")
}