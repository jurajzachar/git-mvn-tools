package com.blueskiron.gitmvn.tools

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.IOException
import org.slf4j.LoggerFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.lib.Ref
import scala.util._
import org.eclipse.jgit.api.CreateBranchCommand
import java.io.File
import org.eclipse.jgit.api.ListBranchCommand

/**
 * @author juraj.zachar@gmail.com
 *
 */
object JGitHelper {

  private val logger = LoggerFactory.getLogger(this.getClass);

  /**
   * @return Repository
   */
  @throws(classOf[IOException])
  def openJGitRepository(): Repository = {
    val builder = new FileRepositoryBuilder()
    builder.readEnvironment().findGitDir().build()
  }

  @throws(classOf[IOException])
  def openJGitRepository(dir: String): Repository = {
    val builder = new FileRepositoryBuilder()
    builder.setMustExist(true).findGitDir(new File(dir)).build()
  }

  /**
   * List local and remote branches
   *
   * @param repo
   * @return
   */
  def listAllBranches(repo: Repository): Try[List[Ref]] = {
    import collection.JavaConverters._
    try {
      logger.debug("Listing local branches...")
      val git = new Git(repo)
      val localCall = git.branchList().call().asScala.toList.map(ref => {
        val localBranch = Repository.shortenRefName(ref.getName())
        logger.debug(s"Found local: $localBranch:${ref.getObjectId().getName()}")
        ref
      })
      logger.debug("Listing remote branches...")
      val remoteCall = git.branchList().setListMode(ListMode.REMOTE).call()
        .asScala.toList.map(ref => {
          logger.debug(s"Found remote branch: ${ref.getName()}:${ref.getObjectId().getName()}")
          ref
        })
      Success(localCall ++ remoteCall)
    } catch {
      case e: IOException => {
        logger.error("Cannot open .git repository. Does it exist?")
        Failure(e)
      }
      case e: Exception => {
        logger.error("Unexpected error: {}", e)
        Failure(e)
      }
    }
  }

  /**
   * @param repo
   * @return a list of all known local branches
   */
  def listLocalBranches(repo: Repository): Try[List[Ref]] = {
    logger.debug("Listing local branches...")
    listBranches(repo, identity)
  }

  /**
   * @param repo
   * @return a list of all known remote branches
   */
  def listRemoteBranches(repo: Repository): Try[List[Ref]] = {
    logger.debug("Listing remote branches...")
    listBranches(repo, listCmd => listCmd.setListMode(ListMode.REMOTE))
  }

  private def listBranches(repo: Repository, branchFunc: ListBranchCommand => ListBranchCommand): Try[List[Ref]] = {
    import collection.JavaConverters._
    try {
      val git = new Git(repo)
      val call = branchFunc(git.branchList()).call()
        .asScala.toList.map(ref => {
          logger.debug(s"Found branch: ${ref.getName()}:${ref.getObjectId().getName()}")
          ref
        })
      Success(call)
    } catch {
      case e: IOException => {
        logger.error("Cannot open .git repository. Does it exist?")
        Failure(e)
      }
      case e: Exception => {
        logger.error("Unexpected error: {}", e)
        Failure(e)
      }
    }
  }

  /**
   * Gets or creates a new GIT branch with the provided name
   *
   * @param name
   * @param branches
   * @param repo
   * @return
   */
  def getOrCreateBranch(name: String, branches: List[Ref], repo: Repository): Either[CreateBranchCommand, Ref] = {
    //branch with the given name already exists?
    branches.find(branch => branch.getName.contains(name)).map(ref => {
      logger.warn(s"Branch '$name' alredy exists!")
      Right(ref)
    }).getOrElse {
      logger.info(s"Creating a new branch '$name'")
      val git = new Git(repo);
      Left(git.branchCreate().setName(name))
    }
  }

  /**
   * Gets 'master' ref wrapped as option
   *
   * @param repo
   * @return
   */
  def maybeMaster(repo: Repository): Option[Ref] = {
    repo.getBranch == "master" match {
      case true  => Some(repo.findRef("master"))
      case false => None
    }
  }
}