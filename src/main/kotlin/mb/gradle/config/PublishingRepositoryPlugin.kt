package mb.gradle.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import java.net.URI

class PublishingRepositoryPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.withPlugin("publishing") {
      project.configure<PublishingExtension> {
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
  }
}
