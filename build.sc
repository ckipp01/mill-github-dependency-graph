import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import scalalib._
import scalafmt._
import publish._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil
import com.goyeau.mill.scalafix.ScalafixModule
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

val millVersions = Seq("0.10.12", "0.11.0-M10")
val millBinaryVersions = millVersions.map(scalaNativeBinaryVersion)
val scala213 = "2.13.10"
val artifactBase = "mill-github-dependency-graph"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

def millVersion(binaryVersion: String) =
  millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

trait Common
    extends ScalaModule
    with CiReleaseModule
    with ScalafixModule
    with ScalafmtModule {

  def pomSettings = PomSettings(
    description = "Submit your mill project's dependency graph to GitHub",
    organization = "io.chris-kipp",
    url = "https://github.com/ckipp01/mill-github-dependency-graph",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl
      .github(owner = "ckipp01", repo = "mill-github-dependency-graph"),
    developers =
      Seq(Developer("ckipp01", "Chris Kipp", "https://www.chris-kipp.io"))
  )

  override def sonatypeHost: Option[SonatypeHost] = Some(SonatypeHost.s01)

  def scalaVersion = scala213

  def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")

  def scalafixScalaBinaryVersion = ZincWorkerUtil.scalaBinaryVersion(scala213)
}

object domain extends Common {
  override def artifactName = "github-dependency-graph-domain"
}

object plugin extends Cross[Plugin](millBinaryVersions: _*)
class Plugin(millBinaryVersion: String) extends Common with BuildInfo {

  override def millSourcePath = super.millSourcePath / os.up

  override def sources = T.sources {
    super.sources() ++ Seq(
      millSourcePath / s"src-mill${millVersion(millBinaryVersion).split('.').take(2).mkString(".")}"
    ).map(PathRef(_))
  }

  override def artifactName =
    s"${artifactBase}_mill${millBinaryVersion}"

  override def moduleDeps = Seq(domain)
  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion(millBinaryVersion)}"
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.lihaoyi::upickle:3.1.0",
    ivy"com.lihaoyi::requests:0.8.0",
    ivy"com.github.package-url:packageurl-java:1.4.1"
  )

  override def buildInfoMembers = Map(
    "detectorName" -> artifactBase,
    "homepage" -> pomSettings().url,
    "version" -> publishVersion()
  )
  override def buildInfoObjectName = "BuildInfo"
  override def buildInfoPackageName = Some(
    "io.kipp.mill.github.dependency.graph"
  )
}

object itest extends Cross[ItestCross](millVersions: _*)
class ItestCross(millVersion: String) extends MillIntegrationTestModule {

  override def millSourcePath = super.millSourcePath / os.up

  def millTestVersion = millVersion

  def pluginsUnderTest = Seq(plugin(millBinaryVersion(millVersion)))

  def testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    T {
      Seq(
        PathRef(testBase / "minimal") -> Seq(
          TestInvocation.Targets(Seq("checkManifest"), noServer = true)
        ),
        PathRef(testBase / "directRelationship") -> Seq(
          TestInvocation.Targets(Seq("verify"), noServer = true)
        ),
        PathRef(testBase / "eviction") -> Seq(
          TestInvocation.Targets(Seq("verify"), noServer = true)
        ),
        PathRef(testBase / "range") -> Seq(
          TestInvocation.Targets(Seq("verify"), noServer = true)
        ),
        PathRef(testBase / "reconciledRange") -> Seq(
          TestInvocation.Targets(Seq("verify"), noServer = true)
        ),
        PathRef(testBase / "cyclical") -> Seq(
          TestInvocation.Targets(Seq("checkManifest"), noServer = true)
        )
      )
    }
}
