package test.com.blueskiron.gitmvn.tools

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory
import com.blueskiron.gitmvn.tools.ReleaseHelper.DevBranchName
import com.blueskiron.gitmvn.tools.MavenHelper.SemanticVersion
import com.blueskiron.gitmvn.tools.ReleaseHelper
import org.eclipse.jgit.lib.Ref
import scala.util.Success
import com.blueskiron.gitmvn.tools.ReleaseHelper.Options
import com.blueskiron.gitmvn.tools.MavenHelper.MavenArtifact

class ReleaseHelperTestSuite extends FlatSpec with Matchers  {
  
  val logger = LoggerFactory.getLogger(this.getClass)
  
  "ReleaseHelper" should "display correct branch name" in {
    {
      val expectedMajorBranchName = "v1.x"
      val branchName = DevBranchName(1, None, None)
      logger.debug(s"generated major branch name: $branchName")
      branchName.toString shouldBe expectedMajorBranchName
    }
    {
      val expectedMajorBranchName = "v1.0.x"
      val branchName = DevBranchName(1, Some(0), None)
      logger.debug(s"generated major branch name: $branchName")
      branchName.toString shouldBe expectedMajorBranchName
    }
  }
  
  "BugfixRelease" should "yield correct next version and dev branch name" in {
    val currentSemanticVersion = SemanticVersion(1, 0, 0, false)
    val expectedSemanticVersion = SemanticVersion(1, 0, 1, true)
    val currentBugfixBranchName = "v1.0.x"
    val expectedBugfixBranchName = "v1.0.x"
    val nextSemanticVersion = ReleaseHelper.BugFixRelease.nextVersion(currentSemanticVersion)
    logger.debug(s"[BugFixRelease] next version: $nextSemanticVersion")
    nextSemanticVersion shouldBe expectedSemanticVersion
    val nextMavenArtifact = MavenArtifact("foo.bar", "bar", expectedSemanticVersion)
    val nextDevBranch = ReleaseHelper.BugFixRelease.nextDevBranch(nextMavenArtifact)
    logger.debug(s"[BugFixRelease] next dev branch name: $nextDevBranch")
    nextDevBranch.toString shouldBe expectedBugfixBranchName
  }
  
  "MinorRelease" should "yield correct next version and dev branch name" in {
    //test for incremental minor dev branch name
      val currentSemanticVersion = SemanticVersion(1, 0, 0, false)
      val expectedSemanticVersion = SemanticVersion(1, 1, 0, true)
      val currentMinorBranchName = "v1.0.x"
      val expectedMinorBranchName = "v1.1.x"
      val nextSemanticVersion = ReleaseHelper.MinorRelease.nextVersion(currentSemanticVersion)
      logger.debug(s"[MinorRelease] next version: $nextSemanticVersion")
      nextSemanticVersion shouldBe expectedSemanticVersion
      val nextMavenArtifact = MavenArtifact("foo.bar", "bar", expectedSemanticVersion)
      val nextDevBranch = ReleaseHelper.MinorRelease.nextDevBranch(nextMavenArtifact)
      logger.debug(s"[MinorRelease] next dev branch name: $nextDevBranch")
      nextDevBranch.toString shouldBe expectedMinorBranchName
  }
  
  "MinorRelease" should "yield correct next version and the same dev branch name" in {
     //test for non-incremental minor dev branch name
      val currentSemanticVersion = SemanticVersion(1, 0, 0, false)
      val expectedSemanticVersion = SemanticVersion(1, 1, 0, true)
      val currentMinorBranchName = "v1.x"
      val expectedMinorBranchName = "v1.1.x"
      val nextSemanticVersion = ReleaseHelper.MinorRelease.nextVersion(currentSemanticVersion)
      logger.debug(s"[MinorRelease] next version: $nextSemanticVersion")
      nextSemanticVersion shouldBe expectedSemanticVersion
      val nextMavenArtifact = MavenArtifact("foo.bar", "bar", expectedSemanticVersion)
      val nextDevBranch = ReleaseHelper.MinorRelease.nextDevBranch(nextMavenArtifact)
      logger.debug(s"[MinorRelease] next dev branch name: $nextDevBranch")
      nextDevBranch.toString shouldBe expectedMinorBranchName
  }
  
  "MajorRelease" should "yield correct next version and the dev branch name" in {
     //test for non-incremental minor dev branch name
      val currentSemanticVersion = SemanticVersion(1, 45, 8, false)
      val expectedSemanticVersion = SemanticVersion(2, 0, 0, true)
      val currentMajorBranchName = "v1.45.x"
      val expectedMajorBranchName = "v2.0.x"
      val nextSemanticVersion = ReleaseHelper.MajorRelease.nextVersion(currentSemanticVersion)
      logger.debug(s"[MajorRelease] next version: $nextSemanticVersion")
      nextSemanticVersion shouldBe expectedSemanticVersion
      val nextMavenArtifact = MavenArtifact("foo.bar", "bar", expectedSemanticVersion)
      val nextDevBranch = ReleaseHelper.MajorRelease.nextDevBranch(nextMavenArtifact)
      logger.debug(s"[MajorRelease] next dev branch name: $nextDevBranch")
      nextDevBranch.toString() shouldBe expectedMajorBranchName
  }
  
  "RealeaseHelper" should "generate successful report" in {
    val opts = Options("src/test/resources/sample-repo.git", "minor", true, true)
    ReleaseHelper.makeRelease(opts, report => {
      report.display(logger)
      //user input required to confirm, [yes]/no
      true
    }) match {
      case Left(isConfirmed) => logger.info(s"release is confirmed: $isConfirmed") //all left is ok
      case Right(err) => logger.error(s"$err"); fail(err)
    }
  }
  
}