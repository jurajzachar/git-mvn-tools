package test.com.blueskiron.gitmvn.tools

import java.nio.file.Paths
import org.slf4j.LoggerFactory
import org.eclipse.jgit.api.Git

object GitFixture {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val gitDir = Paths.get("target", "test-classes", "sample-repo.git", ".git")
    .toFile()

  val repoDir = gitDir.getParentFile

  def initGitRepoIfNotExists() {
    if (!gitDir.exists()) {
      logger.info(s"initializing a sample git repository in $repoDir...")
      val git = Git.init().setDirectory(repoDir).call()
      val pom = "pom.xml"
      if (git.status().call().getUntracked.contains(pom)) {
        git.add().addFilepattern(pom).call();
        git.commit().setMessage(s"[${this.getClass}] commit pom.xml").call()
      }
    }
  }
}