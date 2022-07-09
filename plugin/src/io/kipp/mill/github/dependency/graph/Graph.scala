package io.kipp.mill.github.dependency.graph

import io.kipp.github.dependency.graph.domain
import mill._
import mill.define.Command
import mill.define.Discover
import mill.define.ExternalModule
import mill.eval.Evaluator
import mill.main.EvaluatorScopt

object Graph extends ExternalModule { outer =>

  import Writers._

  def submit(ev: Evaluator): Command[Unit] = T.command {
    val manifests = generate(ev)()
    val snapshot = Github.snapshot(manifests)
    Github.submit(snapshot)
  }

  def generate(ev: Evaluator): Command[Map[String, domain.Manifest]] =
    T.command {
      val modules = Resolver.computeModules(ev)
      val moduleTrees = Resolver.resolveModuleTrees(ev, modules)
      val manifests: Map[String, domain.Manifest] =
        moduleTrees.map(mt => (mt.module.toString(), mt.toManifest())).toMap

      manifests
    }

  implicit def millScoptEvaluatorReads[T]: EvaluatorScopt[T] =
    new mill.main.EvaluatorScopt[T]()

  lazy val millDiscover: Discover[this.type] = mill.define.Discover[this.type]

}
