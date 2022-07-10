import mill._, scalalib._
import $exec.plugins
import io.kipp.mill.github.dependency.graph.Graph
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimal extends ScalaModule {
  def scalaVersion = "3.1.3"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::pprint:0.7.3",
    ivy"com.lihaoyi::fansi:0.3.0"
  )
}

def verify(ev: Evaluator) = T.command {
  val manifestMapping = Graph.generate(ev)()
  assert(manifestMapping.size == 1)

  // Very similiar to the directRelationship test but in this case fansi 0.3.0
  // _shouldn't_ be marked as direct in the end result. This is because it will
  // end up being evicted by 0.3.1 in pprint, so we don't need to even include
  // it. However, it should end up being replaced by 0.3.1 and still marked as
  // a direct.
  val fansiIsDirect = manifestMapping.head._2.resolved
    .get("com.lihaoyi:fansi_3:0.3.1")
    .exists(_.isDirectDependency)

  assert(fansiIsDirect)
}
