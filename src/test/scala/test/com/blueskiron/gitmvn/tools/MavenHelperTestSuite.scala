package test.com.blueskiron.gitmvn.tools

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.slf4j.LoggerFactory
import com.blueskiron.gitmvn.tools.MavenHelper
import java.nio.file.Paths
import scala.util.Failure
import scala.util.Success

class MavenHelperTestSuite extends FlatSpec with Matchers {

  val logger = LoggerFactory.getLogger(this.getClass)

  val mavenDir = Paths.get("target", "test-classes", "sample-repo.git")
    .toAbsolutePath().toString()

  "MavenHelper" should "be able to update existing pom.xml" in {
    MavenHelper.parsePom(mavenDir) match {
      case Success(mavenArtifact) => {
        logger.info(s"Parsed: $mavenArtifact")
        val updatedArtifact = mavenArtifact.updateVersion(v => v.nextPatch().nextMinor().nextMajor().release())
        logger.info(s"Updated version (after next patch, minor and major and release): ${updatedArtifact.version}")
        MavenHelper.updatePom(mavenDir, updatedArtifact) match {
          case Success(updatedPom) => logger.info(s"${MavenHelper.writePom(mavenDir, updatedPom)}")
          case Failure(t)          => logger.error("eeek!", t); fail(t)
        }
      }
      case Failure(t) => logger.error("failed: ", t); fail(t)
    }
  }
}