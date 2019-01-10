import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.11" // Stick with version 1.3.11 because the kotlin-dsl plugin in Gradle 5.1 uses that.
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
    create("metaborg-config") {
      id = "org.metaborg.gradle.config"
      implementationClass = "mb.gradle.config.MetaborgConfigPlugin"
    }
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
