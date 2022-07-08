import mill._
import scalalib._
import scalafmt._
import publish._
import mill.scalalib.publish._
import $ivy.`com.goyeau::mill-scalafix::0.2.8`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.6.0`
import de.tobiasroeser.mill.integrationtest._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

// TODO Should probably drop this to 0.10.0, but when I did a bunch of stuff breaks
val millVersion = "0.10.5"
val artifactBase = "mill-github-dependency-graph"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

trait Common
    extends ScalaModule
    with PublishModule
    with ScalafixModule
    with ScalafmtModule {

  def publishVersion = VcsVersion.vcsState().format()

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

  def scalaVersion = "2.13.8"

  def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")
}

object domain extends Common {
  override def artifactName = "github-dependency-graph-domain"
}

object plugin extends Common with BuildInfo {

  override def artifactName =
    s"${artifactBase}_mill${millBinaryVersion(millVersion)}"

  override def moduleDeps = Seq(domain)
  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:$millVersion",
    ivy"com.lihaoyi::upickle:2.0.0",
    ivy"com.lihaoyi::requests:0.7.1"
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

object itest extends MillIntegrationTestModule {

  def millTestVersion = millVersion

  def pluginsUnderTest = Seq(plugin)

  def testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    T {
      Seq(
        PathRef(testBase / "minimal") -> Seq(
          TestInvocation.Targets(Seq("checkManifest"), noServer = true),
          TestInvocation.Targets(Seq("checkManifest"), noServer = true)
        )
      )
    }
}
