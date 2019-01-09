package mb.gradle.config

import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

class JavaPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.withPlugin("java-base") {
      project.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
