package com.blueskiron.gitmvn.tools

import scala.util._
import scala.io.Source
import java.io.File
import scala.xml._
import scala.xml.transform._
import org.eclipse.jgit.util.StringUtils
import scala.xml.Elem
import org.slf4j.LoggerFactory
import java.io.PrintWriter

/**
 * @author juraj.zachar@gmail.com
 *
 */
object MavenHelper {

  private val logger = LoggerFactory.getLogger(this.getClass)

  object SemanticVersion {
    def parse(version: String): Try[SemanticVersion] = {
      val tokens = version.split("\\.")
      if (tokens.size != 3) {
        Failure(new Exception(s"cannot parse semantic version string: $version\n --> $tokens"))
      } else {
        val iter = tokens.iterator;
        val major = Integer.parseInt(iter.next())
        val minor = Integer.parseInt(iter.next())
        val patchAndSnapshot = iter.next().split("-")
        val patch = Integer.parseInt(patchAndSnapshot(0))
        if (patchAndSnapshot.size == 1) {
          Success(SemanticVersion(major, minor, patch, false))
        } else {
          Success(SemanticVersion(major, minor, patch, true))
        }
      }
    }
  }

  case class SemanticVersion(major: Int, minor: Int, patch: Int, isSnapshot: Boolean) {
    def nextMajor() = SemanticVersion(major + 1, 0, 0, isSnapshot)
    def nextMinor() = SemanticVersion(major, minor + 1, patch, isSnapshot)
    def nextPatch() = SemanticVersion(major, minor, patch + 1, isSnapshot)
    def release() = SemanticVersion(major, minor, patch, false)
    def snapshot() = SemanticVersion(major, minor, patch, true)
    override def toString() = s"$major.$minor.$patch${if (isSnapshot) "-SNAPSHOT" else ""}"
  }

  case class MavenArtifact(artifactId: String, groupId: String, version: SemanticVersion) {
    
    def updateVersion(f: SemanticVersion => SemanticVersion) = MavenArtifact(artifactId, groupId, f(version))
    
    def justArtifactAsString() = s"$groupId/$artifactId"
    
    override def toString() = s"${justArtifactAsString()}/${version.toString()}"  
  }

  def updatePom(dir: String, artifact: MavenArtifact): Try[String] = {
    object VersionUpdater extends RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        //match version element
        case Elem(prefix, "version", attribs, scope, _*) => <version>{ artifact.version }</version>
        // That which we cannot speak of, we must pass over
        // in silence....
        case other                                       => other
      }
    }
    object PomTransformer extends RuleTransformer(VersionUpdater)

    pomFile(dir).flatMap { pom =>
      val updated = PomTransformer.transform(pom)
      Success(updated.mkString)
    }
  }

  def writePom(dir: String, pom: String): Try[Unit] = {
    val file = new File(s"$dir/pom.xml")
    try {
      if (!file.exists()) {
        logger.warn("creating a new pom file! is this expected?")
        file.createNewFile()
      }
      val writer = new PrintWriter(file)
      writer.write(pom)
      writer.close()
      Success()
    } catch {
      case e: Exception => Failure(e)
    }
  }

  def parsePom(dir: String): Try[MavenArtifact] = {
    pomFile(dir).flatMap(pom => {
      val artifactId: String = pom \\ "project" \ "artifactId" text
      val groupId: String = pom \\ "project" \ "groupId" text
      val version: String = pom \\ "project" \ "version" text

      SemanticVersion.parse(version).flatMap(semanticVersion => Success(MavenArtifact(artifactId, groupId, semanticVersion)))
    })
  }

  def pomFile(dir: String): Try[Elem] = {
    val file = new File(s"$dir/pom.xml")
    if (!file.exists()) {
      Failure(new Exception(s"file: ${file.getAbsolutePath} does not exist!"))
    } else {
      try {
        val elem = XML.loadFile(file)
        Success(elem)
      } catch {
        case e: Exception => Failure(e)
      }
    }
  }

}