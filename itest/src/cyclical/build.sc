import mill._, scalalib._
import $file.plugins
import io.kipp.mill.github.dependency.graph.Graph
import io.kipp.mill.github.dependency.graph.Writers._
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object overflow extends ScalaModule {

  def scalaVersion = "2.13.10"

  // See https://github.com/ckipp01/mill-github-dependency-graph/issues/77 for the context
  // of this test. The main issue is that when you look at the children of this dep in coursier
  // it is cyclical and will just continually list itself. So we have to add in some extra guards
  // against this.
  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"io.netty:netty-tcnative-boringssl-static:2.0.54.Final"
  )

}

def checkManifest(ev: Evaluator) = T.command {
  val expected = ujson.read(os.read(os.pwd / "manifests.json"))

  val manifestMapping = Graph.generate(ev)()

  // Lil hacky but if we compare the strings they won't match, so we read that
  // back up into a ujson.Value so we can compare those two
  val result = ujson.read(upickle.default.write(manifestMapping))

  assertEquals(result, expected)
}
