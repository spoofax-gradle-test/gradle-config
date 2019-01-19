package mb.gradle.config

import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.*
import org.gradle.api.*
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
        for((name, includeOverride, urlOverride, branchOverride, dirPathOverride) in extension.repos) {
          val include = includeOverride ?: "true" == properties.getProperty("$name.include")
          if(!include) continue
          val url = urlOverride ?: properties.getProperty("$name.url") ?: "$urlPrefix/$name.git"
          val branch = branchOverride ?: properties.getProperty("$name.branch") ?: rootBranch
          val dirName = dirPathOverride ?: properties.getProperty("$name.dir") ?: name
          val dir = projectDir.resolve(dirName)

          val sshSessionFactory = object : JschConfigSessionFactory() {
            override fun configure(host: OpenSshConfig.Host, session: Session) {
              session.userInfo =
            }
          }

          // Clone or open the repository.
          if(!dir.exists()) {
            val clone = CloneCommand()
            clone.setDirectory(dir)
            clone.setURI(url)
            clone.setBranch(branch)
            clone.setCloneSubmodules(true)
            clone.setTransportConfigCallback(TransportConfigCallback { transport ->
              if(transport is SshTransport) {
                transport.sshSessionFactory = sshSessionFactory
              }
            })
            try {
              clone.call()
            } catch(e: GitAPIException) {
              throw GradleException("Cannot update repositories of devenv; cloning '$url' into '$dir' failed unexpectedly", e)
            }
          } else {
            val repo = try {
              FileRepositoryBuilder().readEnvironment().findGitDir(dir).setMustExist(true).build()
            } catch(e: RepositoryNotFoundException) {
              throw GradleException("Cannot update repositories of devenv; no git repository was found at '$dir'", e)
            }
            Git(repo)
          }.use { git ->
            val repo = git.repository
            if(branch != repo.branch) {
              // Check repository out to the correct branch.
              val checkout = git.checkout()
              checkout.setName(branch)
              checkout.call()
            }
            // Pull from remote.
            val pull = git.pull()
            pull.setRebase(true)
            pull.setTransportConfigCallback(TransportConfigCallback { transport ->
              if(transport is SshTransport) {
                transport.sshSessionFactory = sshSessionFactory
              }
            })
            try {
              pull.call()
            } catch(e: GitAPIException) {
              throw GradleException("Cannot update repositories of devenv; pulling '$name' failed unexpectedly", e)
            } catch(e: JSchException) {

            }
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
