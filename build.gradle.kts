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
  compile("org.metaborg:gitonium:0.3.0")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
tasks.withType<KotlinCompile>().all {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("publishing-repository") {
      id = "org.metaborg.gradle.config.publishing-repository"
      implementationClass = "mb.gradle.config.PublishingRepositoryPlugin"
    }
    create("composite-build-tasks") {
      id = "org.metaborg.gradle.config.composite-build-tasks"
      implementationClass = "mb.gradle.config.CompositeBuildTasksPlugin"
    }
    create("group-config") {
      id = "org.metaborg.gradle.config.group-config"
      implementationClass = "mb.gradle.config.GroupConfigPlugin"
    }
    create("version-config") {
      id = "org.metaborg.gradle.config.version-config"
      implementationClass = "mb.gradle.config.VersionConfigPlugin"
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
