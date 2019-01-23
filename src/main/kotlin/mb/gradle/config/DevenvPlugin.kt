package mb.gradle.config

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Suppress("unused")
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
      description = "For each Git repository of devenv for which update is set to true: check out the repository to the correct branch and pull from origin, or clone the repository if it has not been cloned yet."
      doLast {
        val projectDir = project.projectDir
        val urlPrefix = extension.repoUrlPrefix
          ?: throw GradleException("Cannot update repositories of devenv; URL prefix has not been set")
        val rootBranch = try {
          gitBranch(projectDir)
        } catch(e: GradleException) {
          throw GradleException("Cannot update repositories of devenv; current branch cannot be retrieved", e)
        }
        val properties = run {
          val file = projectDir.resolve("repo.properties").toPath()
          properties(file)
        }
        for(repoConfig in extension.repos) {
          val repo = toRepo(repoConfig, urlPrefix, rootBranch, properties)
          updateGitRepo(repo, project)
        }
      }
    }
    project.tasks.register("listRepos") {
      group = "devenv"
      description = "Lists the Git repositories of devenv and their properties."
      doLast {
        val projectDir = project.projectDir
        val urlPrefix = extension.repoUrlPrefix
          ?: throw GradleException("Cannot list repositories of devenv; URL prefix has not been set")
        val rootBranch = try {
          gitBranch(projectDir)
        } catch(e: GradleException) {
          throw GradleException("Cannot list repositories of devenv; current branch cannot be retrieved", e)
        }
        val properties = run {
          val file = projectDir.resolve("repo.properties").toPath()
          properties(file)
        }
        println("Git URL prefix: $urlPrefix")
        println("Current branch: $rootBranch")
        println("Repositories:")
        for(repoConfig in extension.repos) {
          val repo = toRepo(repoConfig, urlPrefix, rootBranch, properties)
          println(repo)
        }
      }
    }
  }

  private fun gitBranch(dir: File): String {
    return try {
      FileRepositoryBuilder().readEnvironment().findGitDir(dir).setMustExist(true).build()
    } catch(e: RepositoryNotFoundException) {
      throw GradleException("Cannot retrieve current branch name because no git repository was found at '$dir'", e)
    }.use { repo ->
      // Use repository with 'use' to close repository after use, freeing up resources.
      val headRef = repo.exactRef(Constants.HEAD)
        ?: throw GradleException("Cannot retrieve current branch name because repository has no HEAD")
      if(headRef.isSymbolic) {
        Repository.shortenRefName(headRef.target.name)
      } else {
        throw GradleException("Cannot retrieve current branch name because repository HEAD is not symbolic")
      }
    }
  }

  private fun properties(file: Path): Properties {
    val properties = Properties()
    if(!Files.isRegularFile(file)) {
      throw GradleException("Cannot update repositories of devenv; property file '$file' does not exist or is not a file")
    }
    Files.newInputStream(file).buffered().use { inputStream ->
      properties.load(inputStream)
    }
    return properties
  }

  private fun toRepo(repoConfig: RepoConfig, urlPrefix: String, rootBranch: String, properties: Properties): Repo {
    val (name, defaultUpdate, defaultUrl, defaultBranch, defaultDirPath) = repoConfig
    val update = "true" == properties.getProperty(name) ?: defaultUpdate ?: false
    val url = properties.getProperty("$name.url") ?: defaultUrl ?: "$urlPrefix/$name.git"
    val branch = properties.getProperty("$name.branch") ?: defaultBranch ?: rootBranch
    val dirName = properties.getProperty("$name.dir") ?: defaultDirPath ?: name
    return Repo(name, update, url, branch, dirName)
  }

  private fun updateGitRepo(repo: Repo, project: Project) {
    val (_, update, url, branch, dirPath) = repo
    if(!update) return
    val dir = project.projectDir.resolve(dirPath)
    if(!dir.exists()) {
      println("Cloning repository $dirPath:")
      project.exec {
        executable = "git"
        args = mutableListOf("clone", "--quiet", "--recurse-submodules", "--branch", branch, url, dirPath)
        println(commandLine.joinToString(separator = " "))
      }
    } else {
      println("Updating repository $dirPath:")
      project.exec {
        executable = "git"
        workingDir = dir
        args = mutableListOf("checkout", "--quiet", branch)
        println(commandLine.joinToString(separator = " "))
      }
      project.exec {
        executable = "git"
        workingDir = dir
        args = mutableListOf("pull", "--quiet", "--recurse-submodules", "--rebase")
        println(commandLine.joinToString(separator = " "))
      }
    }
    println()
  }
}


data class Repo(val name: String, val update: Boolean, val url: String, val branch: String, val dirPath: String) {
  override fun toString(): String {
    return String.format("  %1$-30s : update = %2$-5s, branch = %3$-20s, path = %4$-30s, url = %5\$s", name, update, branch, dirPath, url)
  }
}

data class RepoConfig(val name: String, val defaultUpdate: Boolean?, val defaultUrl: String?, val defaultBranch: String?, val defaultDirPath: String?)

@Suppress("unused")
open class DevenvExtension(private val project: Project) {
  internal val repos = mutableListOf<RepoConfig>()

  var repoUrlPrefix: String? = null

  @JvmOverloads
  fun registerRepo(name: String, defaultUpdate: Boolean? = null, defaultUrl: String? = null, defaultBranch: String? = null, defaultDirPath: String? = null) {
    repos.add(RepoConfig(name, defaultUpdate, defaultUrl, defaultBranch, defaultDirPath))
  }

  fun registerCompositeBuildTask(name: String, description: String) {
    project.tasks.register(name) {
      this.group = "composite build"
      this.description = description
      this.dependsOn(project.gradle.includedBuilds.map { it.task(":$name") })
    }
  }
}
