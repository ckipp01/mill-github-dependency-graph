import mill._, scalalib._
import $exec.plugins
import io.kipp.mill.github.dependency.graph.Graph
import mill.eval.Evaluator
import $ivy.`org.scalameta::munit:0.7.29`
import munit.Assertions._

object minimalDep extends ScalaModule {
  // scala-steward:off
  def scalaVersion = "2.13.8"

  def ivyDeps = Agg(
    ivy"com.fasterxml.jackson.core:jackson-core:2.13.3"
  )
}

object minimal extends ScalaModule {
  def moduleDeps = Seq(`minimalDep`)

  def scalaVersion = "2.13.8"

  def ivyDeps = Agg(
    ivy"org.jongo:jongo:1.5.0"
  )
}

def verify(ev: Evaluator) = T.command {
  val manifestMapping = Graph.generate(ev)()
  assert(manifestMapping.size == 2)

  // OK, this is a super weird one. Jongo here has a range dep which is fine, and when
  // it's resolved by itself, it correctly gets 2.12.3 like you can see below:
  //
  // ❯ cs resolve org.jongo:jongo:1.5.0
  // com.fasterxml.jackson.core:jackson-annotations:2.12.3:default
  // com.fasterxml.jackson.core:jackson-core:2.12.3:default
  // com.fasterxml.jackson.core:jackson-databind:2.12.3:default
  // de.undercouch:bson4jackson:2.12.0:default
  // org.jongo:jongo:1.5.0:default
  //
  // The issue enteres with a setup like the above. For some reason coursier
  // will actually retain the range as the reconciledVersion in the
  // DependencyTree when it should be the actual reconciled versions.
  //
  // dep version: [2.7.0,2.12.3]
  // retained version: [2.7.0,2.12.3]
  // reconciled version: [2.7.0,2.12.3]
  //
  // Since I believe this to be a bug in coursier for now we'll just throw them
  // out to ensure we're not creating invalid PURLs.
  val expected = Set(
    "org.scala-lang:scala-library:2.13.8",
    "com.fasterxml.jackson.core:jackson-core:2.13.3",
    // NOTICE that com.fasterxml.jackson.core:jackson-core:[2.7.0,2.12.3] is not here
    "com.fasterxml.jackson.core:jackson-core:2.12.3",
    "com.fasterxml.jackson.core:jackson-databind:2.12.3",
    "com.fasterxml.jackson.core:jackson-annotations:2.12.3",
    "org.jongo:jongo:1.5.0",
    "de.undercouch:bson4jackson:2.12.0"
    // scala-steward:on
  )

  val results = manifestMapping.foldLeft[Set[String]](Set.empty) {
    case (set, mapping) =>
      set ++ mapping._2.resolved.keySet
  }

  assertEquals(results, expected)
}
