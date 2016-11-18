import com.typesafe.sbt.SbtPgp.autoImportImpl._
import de.heikoseeberger.sbtheader.license.Apache2_0
import PgpKeys.gpgCommand
import de.heikoseeberger.sbtheader.HeaderKey._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseTransformations._
import sbt.Keys._
import sbt._
import sbt.ScriptedPlugin._

object BuildCommon extends AutoPlugin {

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectSettings =
    artifactSettings ++
      testScriptedSettings ++
      miscSettings ++
      releaseProcessSettings ++
      pgpSettings ++
      credentialSettings ++
      publishSettings

  private[this] val artifactSettings = Seq(
    name := "sbt-catalysts-extras",
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    homepage := Option(url("http://www.47deg.com")),
    organizationHomepage := Some(new URL("http://47deg.com")),
    headers := Map(
      "scala" -> Apache2_0("2016", "47 Degrees, LLC. <http://www.47deg.com>")
    )
  )

  private[this] val gitUrl = "https://github.com/47deg/sbt-catalysts-extras"

  private[this] val publishSettings = Seq(
    licenses += ("Apache License", url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo := Some(ScmInfo(url(gitUrl), s"scm:git:$gitUrl.git")),
    apiURL := Some(url(gitUrl)),
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := Function.const(false),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("Snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    },
    autoAPIMappings := true
  )

  private[this] val testScriptedSettings =
    ScriptedPlugin.scriptedSettings ++ Seq(
      scriptedDependencies := (compile in Test) map { (analysis) =>
        Unit
      },
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:MaxPermSize=512M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dplugin.version=" + version.value,
            "-Dscala.version=" + scalaVersion.value
          )
      }
    )

  private[this] val gpgFolder = sys.env.getOrElse("PGP_FOLDER", ".")

  private[this] val pgpSettings = Seq(
    pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray),
    gpgCommand := gpgFolder,
    pgpPublicRing := file(s"$gpgFolder/pubring.gpg"),
    pgpSecretRing := file(s"$gpgFolder/secring.gpg")
  )

  private[this] val credentialSettings = Seq(
    credentials ++= (for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield
      Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
  )

  private[this] val releaseProcessSettings = Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges)
  )

  private[this] val miscSettings = Seq(
    shellPrompt := { s: State =>
      val c     = scala.Console
      val blue  = c.RESET + c.BLUE + c.BOLD
      val white = c.RESET + c.BOLD

      val projectName = Project.extract(s).currentProject.id

      s"$blue$projectName$white>${c.RESET}"
    }
  )
}
