import mill._, scalalib._
import $file.plugins
import io.kipp.mill.github.dependency.graph.Graph
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "3.1.3"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::pprint:0.7.3",
    ivy"com.lihaoyi::fansi:0.3.1"
  )
}

def verify(ev: Evaluator) = T.command {
  val manifestMapping = Graph.generate(ev)()
  assert(manifestMapping.size == 1)

  // We want to ensure that fansi here is correctly getting marked as direct
  // since it will appear as a transitive of pprint being marked as indirect
  // first, but then should be updated when fansi is processed as a root node
  // to direct. Also note that it stays as direct because it's not getting
  // evicted. Check out the evicted test for more info.
  val fansiIsDirect = manifestMapping.head._2.resolved
    .get("com.lihaoyi:fansi_3:0.3.1")
    .exists(_.isDirectDependency)

  assert(fansiIsDirect)
}
