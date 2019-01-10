package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      project.tasks.withType<KotlinCompile>().all {
        kotlinOptions.jvmTarget = "1.8"
      }
      val compile by project.configurations
      project.dependencies {
        compile(kotlin("stdlib"))
      }
    }

    project.pluginManager.withPlugin("maven-publish") {
      project.configure<PublishingExtension> {
        publications {
          if(this.findByName("Java") == null) {
            create<MavenPublication>("Java") {
              from(project.components["java"])
            }
          }
        }
      }
    }
  }
}
