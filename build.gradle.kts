plugins {
  id("org.metaborg.gradle.config") version "0.4.3" // Bootstrap with previous version.
  id("org.metaborg.gitonium") version "0.3.0"
  kotlin("jvm") version "1.3.11" // Use version 1.3.11 for compatibility with Gradle 5.1.
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
}

dependencies {
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
  compileOnly("org.metaborg:gitonium:0.3.0")
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.11")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
gradlePlugin {
  plugins {
    create("metaborg-config") {
      id = "org.metaborg.gradle.config"
      implementationClass = "mb.gradle.config.MetaborgConfigPlugin"
    }
  }
  plugins {
    create("metaborg-devenv") {
      id = "org.metaborg.gradle.config.devenv"
      implementationClass = "mb.gradle.config.DevenvPlugin"
    }
  }
}
