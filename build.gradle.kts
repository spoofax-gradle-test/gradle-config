import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.10" // Stick with version 1.3.10 because the kotlin-dsl plugin uses that.
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
  id("org.metaborg.gitonium") version "0.3.0"
}

group = "org.metaborg"

repositories {
  maven(url = "http://home.gohla.nl:8091/artifactory/all/")
}

dependencies {
  compile(kotlin("stdlib"))
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
tasks.withType<KotlinCompile>().all {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("metaborg-publishing") {
      id = "org.metaborg.gradle.config.publishing"
      implementationClass = "mb.gradle.config.PublishingPlugin"
    }
  }
}

tasks.withType<Test> {
  useJUnitPlatform {
    excludeTags.add("longRunning")
  }
}

tasks {
  register("buildAll") {
    dependsOn("build")
  }
  register("cleanAll") {
    dependsOn("clean")
  }
}

publishing {
  repositories {
    maven {
      name = "Artifactory"
      url = uri("http://home.gohla.nl:8091/artifactory/all/")
      credentials {
        username = project.findProperty("publish.repository.Artifactory.username")?.toString()
        password = project.findProperty("publish.repository.Artifactory.password")?.toString()
      }
    }
  }
}
