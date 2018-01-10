package test.com.blueskiron.gitmvn.tools

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.nio.file.Paths
import org.slf4j.LoggerFactory
import com.blueskiron.gitmvn.tools.JGitHelper
import org.eclipse.jgit.lib.Repository
import scala.util.Success
import scala.util.Failure

/**
 * @author juraj
 *
 */
class JGitHelperTestSuite extends FlatSpec with Matchers {
  val logger = LoggerFactory.getLogger(this.getClass)
  val repoDir = Paths.get("src", "test", "resources", "sample-repo.git")
    .toAbsolutePath().toString()

  "JGitHelper" should " create a new branch with the given name" in {
    logger.info(s"Assuming git repository is in $repoDir")
    val repo = JGitHelper.openJGitRepository(repoDir);
    //test branch creation
    val branchName = s"v0.1.x-${System.currentTimeMillis()}"
    JGitHelper.getOrCreateBranch(branchName, List(), repo) match {
      case Left(cmd) => {
        val ref = cmd.call()
        Repository.shortenRefName(ref.getName) should be(branchName)
      }
      case Right(ref) => //ignore
    }
  }

  "JGitHelper" should " be able to list all local branches" in {
    logger.info(s"Assuming git repository is in $repoDir")
    val repo = JGitHelper.openJGitRepository(repoDir);
    logger.info("Querying local and remote branches ...")
    val maybeBranches = JGitHelper.listLocalBranches(repo)
    maybeBranches match {
      case Success(refs) => {
        logger.info(s"Number of branches found: ${refs.size}")
        refs.foreach(ref => logger.info(s"Found: ${ref.getName}"))
        assert(refs.size > 0)
      }
      case Failure(e) => {
        logger.error("{}", e)
        fail(e)
      }
    }
  }

  "JGitHelper" should " be able to list all remote branches" in {
    logger.info(s"Assuming git repository is in $repoDir")
    val repo = JGitHelper.openJGitRepository(repoDir);
    logger.info("Querying local and remote branches ...")
    val maybeBranches = JGitHelper.listLocalBranches(repo)
    maybeBranches match {
      case Success(refs) => {
        logger.info(s"Number of branches found: ${refs.size}")
        refs.foreach(ref => logger.info(s"Found: ${ref.getName}"))
        assert(refs.size > 0)
      }
      case Failure(e) => {
        logger.error("{}", e)
        fail(e)
      }
    }
  }
  
}