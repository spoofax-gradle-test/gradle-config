plugins {
  id("org.metaborg.gradle.config.root-project") version "0.5.0" // Bootstrap with previous version.
  id("org.metaborg.gitonium") version "0.3.0"
  kotlin("jvm") version "1.3.11" // Use version 1.3.11 for compatibility with Gradle 5.1.
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
}

dependencies {
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.11")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
gradlePlugin {
  plugins {
    create("metaborg-root-project") {
      id = "org.metaborg.gradle.config.root-project"
      implementationClass = "mb.gradle.config.RootProjectPlugin"
    }
    create("metaborg-sub-project") {
      id = "org.metaborg.gradle.config.sub-project"
      implementationClass = "mb.gradle.config.SubProjectPlugin"
    }

    create("metaborg-java-library") {
      id = "org.metaborg.gradle.config.java-library"
      implementationClass = "mb.gradle.config.JavaLibraryPlugin"
    }
    create("metaborg-java-application") {
      id = "org.metaborg.gradle.config.java-application"
      implementationClass = "mb.gradle.config.JavaApplicationPlugin"
    }
    create("metaborg-kotlin-library") {
      id = "org.metaborg.gradle.config.kotlin-library"
      implementationClass = "mb.gradle.config.KotlinLibraryPlugin"
    }

    create("metaborg-devenv") {
      id = "org.metaborg.gradle.config.devenv"
      implementationClass = "mb.gradle.config.DevenvPlugin"
    }
  }
}
