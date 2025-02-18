import mill._, scalalib._
import $file.plugins
import io.kipp.mill.github.dependency.graph.Graph
import io.kipp.mill.github.dependency.graph.Writers._
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "3.1.3"

  def ivyDeps = Agg(ivy"com.lihaoyi::pprint:0.7.3")

  object test extends ScalaModuleTests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:0.7.29")
  }
}

def checkManifest(ev: Evaluator) = T.command {
  val projectDir = Iterator
    .iterate(os.pwd)(_ / os.up)
    .find(dir => os.exists(dir / "manifests.json"))
    .get
  val expected = ujson.read(os.read(projectDir / "manifests.json"))

  val manifestMapping = Graph.generate(ev)()

  // Lil hacky but if we compare the strings they won't match, so we read that
  // back up into a ujson.Value so we can compare those two
  val result = ujson.read(upickle.default.write(manifestMapping))

  assertEquals(result, expected)
}
