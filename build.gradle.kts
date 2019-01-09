import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.11" // Stick with version 1.3.11 because the kotlin-dsl plugin uses that.
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
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied by users.
  compileOnly("org.metaborg:gitonium:0.3.0")
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.11")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
tasks.withType<KotlinCompile>().all {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("group-config") {
      id = "org.metaborg.gradle.config.group"
      implementationClass = "mb.gradle.config.GroupPlugin"
    }
    create("version-config") {
      id = "org.metaborg.gradle.config.version"
      implementationClass = "mb.gradle.config.VersionPlugin"
    }
    create("repositories-config") {
      id = "org.metaborg.gradle.config.repositories"
      implementationClass = "mb.gradle.config.RepositoriesPlugin"
    }
    create("composite-build-tasks") {
      id = "org.metaborg.gradle.config.composite-build-tasks"
      implementationClass = "mb.gradle.config.CompositeBuildTasksPlugin"
    }
    create("publishing-repositories") {
      id = "org.metaborg.gradle.config.publishing-repositories"
      implementationClass = "mb.gradle.config.PublishingRepositoriesPlugin"
    }

    create("java") {
      id = "org.metaborg.gradle.config.java"
      implementationClass = "mb.gradle.config.JavaPlugin"
    }
    create("kotlin") {
      id = "org.metaborg.gradle.config.kotlin"
      implementationClass = "mb.gradle.config.KotlinPlugin"
    }

    create("project") {
      id = "org.metaborg.gradle.config.project"
      implementationClass = "mb.gradle.config.project.ProjectPlugin"
    }
    create("multi-project-root") {
      id = "org.metaborg.gradle.config.multi-project-root"
      implementationClass = "mb.gradle.config.project.MultiProjectRootPlugin"
    }
    create("sub-project") {
      id = "org.metaborg.gradle.config.sub-project"
      implementationClass = "mb.gradle.config.project.SubProjectPlugin"
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
