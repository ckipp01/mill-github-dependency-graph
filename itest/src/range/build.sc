import mill._, scalalib._
import $exec.plugins
import io.kipp.mill.github.dependency.graph.Graph
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "3.1.3"

  def ivyDeps = Agg(
    ivy"org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.12.0"
  )
}

def verify(ev: Evaluator) = T.command {
  val manifestMapping = Graph.generate(ev)()
  assert(manifestMapping.size == 1)

  // We want to ensure that the transitive dependency of the above, which has a
  // range dependency doesn't end up in the actualy manifest as a range, but
  // the reconciled version. So we want to ensure `2.8.9` and not
  // `[2.8.6,2.0)`.
  val expected = Set(
    "org.scala-lang:scala-library:2.13.10",
    "org.scala-lang:scala3-library_3:3.1.3",
    "org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.12.0",
    "com.google.code.gson:gson:2.8.9"
  )

  assertEquals(manifestMapping.head._2.resolved.keys, expected)
}
