package mb.gradle.config

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Files
import java.util.*

class DevenvPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = DevenvExtension(project)
    project.extensions.add("devenv", extension)
    project.afterEvaluate {
      configure(this, extension)
    }
  }

  private fun configure(project: Project, extension: DevenvExtension) {
    project.tasks.register("updateRepos") {
      group = "devenv"
      doLast {
        val projectDir = project.projectDir
        val urlPrefix = extension.repoUrlPrefix
          ?: throw GradleException("Cannot update all repositories of devenv; URL prefix has not been set")
        val rootBranch = run {
          try {
            FileRepositoryBuilder().readEnvironment().findGitDir(projectDir).setMustExist(true).build()
          } catch(e: RepositoryNotFoundException) {
            throw GradleException("Cannot update repositories of devenv; cannot retrieve current branch name because no git repository was found at '$projectDir'", e)
          }.use { repo ->
            // Use repository with 'use' to close repository after use, freeing up resources.
            val headRef = repo.exactRef(Constants.HEAD)
              ?: throw GradleException("Cannot update repositories of devenv; cannot retrieve current branch name because repository has no HEAD")
            if(headRef.isSymbolic) {
              Repository.shortenRefName(headRef.target.name)
            } else {
              throw GradleException("Cannot update repositories of devenv; cannot retrieve current branch name because repository HEAD is not symbolic")
            }
          }
        }
        val properties = run {
          val properties = Properties()
          val propertiesFile = projectDir.resolve("repo.properties").toPath()
          if(!Files.isRegularFile(propertiesFile)) {
            throw GradleException("Cannot update repositories of devenv; property file '$propertiesFile' does not exist or is not a file")
          }
          Files.newInputStream(propertiesFile).buffered().use { inputStream ->
            properties.load(inputStream)
          }
          properties
        }
        // Update all Git repositories.
        for((name, includeOverride, urlOverride, branchOverride, dirPathOverride) in extension.repos) {
          val include = includeOverride ?: "true" == properties.getProperty("$name.include")
          if(!include) continue
          val url = urlOverride ?: properties.getProperty("$name.url") ?: "$urlPrefix/$name.git"
          val branch = branchOverride ?: properties.getProperty("$name.branch") ?: rootBranch
          val dirName = dirPathOverride ?: properties.getProperty("$name.dir") ?: name
          val dir = projectDir.resolve(dirName)
          if(!dir.exists()) {
            project.exec {
              executable = "git"
              args = mutableListOf("clone", "--recurse-submodules", "--branch", branch, url, dirName)
              println(commandLine.joinToString(separator = " "))
            }
          }
          project.exec {
            executable = "git"
            workingDir = dir
            args = mutableListOf("checkout", "-q", branch)
            println("In $dirName: ${commandLine.joinToString(separator = " ")}")
          }
          project.exec {
            executable = "git"
            workingDir = dir
            args = mutableListOf("pull", "--recurse-submodules", "--rebase")
            println("In $dirName: ${commandLine.joinToString(separator = " ")}")
          }
        }
      }
    }
  }
}

data class Repo(val name: String, val includeOverride: Boolean?, val urlOverride: String?, val branchOverride: String?, val dirPathOverride: String?)

open class DevenvExtension(private val project: Project) {
  internal val repos = mutableListOf<Repo>()

  var repoUrlPrefix: String? = null

  @JvmOverloads
  fun registerRepo(name: String, include: Boolean? = null, url: String? = null, branch: String? = null, dirPath: String? = null) {
    repos.add(Repo(name, include, url, branch, dirPath))
  }

  fun registerCompositeBuildTask(name: String, description: String) {
    project.tasks.register(name) {
      this.group = "composite build"
      this.description = description
      this.dependsOn(project.gradle.includedBuilds.map { it.task(":$name") })
    }
  }
}
