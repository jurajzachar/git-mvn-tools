package com.blueskiron.gitmvn.tools

import org.slf4j.Logger
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.slf4j.LoggerFactory

/**
 * @author juraj.zachar@gmail.com
 *
 */
object ReleaseHelper {
  
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  import Repository._
  import JGitHelper._
  import MavenHelper._

  /* 
   * how to make a release
   *
   * x. major/minor/bugfix?
   * x. for major/minor create new dev branch?
   * x. set release number in pom
   * x. next dev branch with SNAPSHOT
   * x. display report
   * x. push changes
   *
   */
  
  /**
   * @param opts options to use for this release
   * @param decision function which take maybe successful release report and either confirms (true) or rejects (false) it.
   */
  def makeRelease(opts: Options, decision: Report => Boolean): Either[Boolean, Throwable] = {
    try {
      logger.debug(s"opening repository under '${opts.dir}'")      
      val repo = openJGitRepository(opts.dir)
      val release = opts.getRelease
      //check we are releasing off master!
      val mavenArtifact = parsePom(opts.dir).getOrElse(throw new IllegalArgumentException(s"'${opts.dir}' does not look like a maven project (no pom.xml found)!"))
      logger.debug(s"will attempt to make a ${release} of ${mavenArtifact.toString}")
      val currentRef = maybeMaster(repo).getOrElse(throw new IllegalArgumentException(s"in '${opts.dir}', it does not look like we are currently working with 'master' ref"))
      logger.debug(s"we are making a new release off ${currentRef} branch")
      listLocalBranches(repo) match {
        case Failure(err) => throw err
        case Success(localBranches) => {
          val releasedMavenArtifact = mavenArtifact.updateVersion(_.release())
          val nextMavenArtifact = releasedMavenArtifact.updateVersion(release.nextVersion(_))
          val sortedBranches = localBranches.filter(!_.getName.equals(currentRef.getName)).map(ref => shortenRefName(ref.getName)).sorted
          logger.debug(s"sorted branches: $sortedBranches")
          //if no previous dev branches found, create a new one
          val currentDevBranchName = sortedBranches.headOption.getOrElse(s"v${mavenArtifact.version.major}.{mavenArtifact.version.minor}.x")
          val nextDevBranch = release.nextDevBranch(nextMavenArtifact)
          val report = Report(mavenArtifact, releasedMavenArtifact, nextMavenArtifact, opts.release, currentDevBranchName, nextDevBranch.toString)
          val isConfirmed = decision(report)
          Left(isConfirmed)
          
        }
      }
    } catch {
      case e: Exception => Right(e)
    }
  }

  case class Options(dir: String, release: String, verbose: Boolean, pushToRemote: Boolean) {
    def getRelease = {
      release.trim.toLowerCase match {
        case "bugfix"       => BugFixRelease
        case "minor"        => MinorRelease
        case "major"        => MajorRelease
        case everythingElse => throw new IllegalArgumentException(s"don't understand what you want to do with '$everythingElse'! expecting [bugfix|minor|major]")
      }
    }
  }

  object DevBranchName {

    import scala.util.matching.Regex
    val regexNumbers = new Regex("""\d+""")
    val vPrefix = new Regex("""(^v)(\d+)""")

    def parse(version: String): Try[DevBranchName] = {
      val tokens = version.split("\\.")
      if (tokens.size < 2) {
        Failure(new IllegalArgumentException(s"cannot parse dev branch string: $version\n --> $tokens"))
      } else {
        try {
          val iter = tokens.iterator;
          val vMajor = iter.next
          val major = Integer.parseInt(vMajor match {
            case vPrefix(v, major) => major
            case _                 => throw new IllegalArgumentException(s"found '$vMajor' but development branches start with 'v' followed by a major version number")
          })
          iter.next match {
            case "x" => Success(DevBranchName(major, None, None))
            case token => {
              regexNumbers findFirstIn token match {
                case Some(minor) => Success(DevBranchName(major, Some(Integer.parseInt(minor)), None))
                case None        => throw new IllegalArgumentException(s"was expecting a number a minor number version identifier, but got instead: '$token'")
              }
            }
          }
        } catch {
          case e: Exception => Failure(e)
        }
      }
    }
    
  }

  /* e.g.
   * branch: v1.x --> can hold artifacts 1.1.0-SNAPSHOT, 1.1.1-SNAPSHOT, 1.2.0-SNAPSHOT, etc.
   * branch v0.1.x --> can hold arfifacts 0.1.0-SNAPSHOT, 0.1.1-SNAPSHOT, 0.1.2-SNAPSHOT, etc
   * */
  case class DevBranchName(major: Int, minor: Option[Int], patch: Option[Int]) {
    
    override def toString() =
      //if minor is set then branch name does not include patch wildcard
      s"v$major." + (minor.map(_ + ".x").getOrElse("x"))

  }

  sealed trait Release {
    
    def nextVersion(current: SemanticVersion): SemanticVersion
    
    def nextDevBranch(nextArtifact: MavenArtifact): DevBranchName = {
      val version = nextArtifact.version
      DevBranchName(version.major, Some(version.minor), None)
    }
  }

  case object BugFixRelease extends Release {

    override def nextVersion(current: SemanticVersion) =
      current.nextPatch().snapshot()
  }

  case object MinorRelease extends Release {

    override def nextVersion(current: SemanticVersion) =
      current.nextMinor().snapshot()
  }

  case object MajorRelease extends Release {

    override def nextVersion(current: SemanticVersion) =
      current.nextMajor().snapshot()

  }

  /**
   * @author juraj
   *
   */
  case class Report(current: MavenArtifact, released: MavenArtifact, next: MavenArtifact, release: String, previousDevBranch: String, currentDevBranch: String) {
    private val format =
      s"""
        
      ===========================================================
      | release summary of '${current.justArtifactAsString}' 
      ===========================================================
      | MAVEN                                                  
      -----------------------------------------------------------
      | current version                      | ${current.version}
      | released ($release) version             | ${released.version}
      | next dev version                     | ${next.version}
      -----------------------------------------------------------
      | GIT                                                    
      -----------------------------------------------------------
      | previous dev branch                  | $previousDevBranch 
      | next dev branch                      | $currentDevBranch  
      -----------------------------------------------------------
      
      """

    def display(log: Logger) {
      log.info(s"$format")
    }
    
    protected def apply(repo: Repository, localBranches: List[Ref]) {
     //1 update pom with release version --> this gets to written to master
     //2.  
    }
  }

}