package io.kipp.mill.github.dependency.graph

import mill._
import mill.define.ExternalModule
import mill.eval.Evaluator
import mill.main.EvaluatorScopt

object Graph extends ExternalModule { outer =>

  import Writers._

  def submit(ev: Evaluator) = T.command {
    val manifests = generate(ev)()
    val snapshot = Github.snapshot(manifests)
    // TODO map to full Snapshot
  }

  def generate(ev: Evaluator) = T.command {
    val modules = Resolver.computeModules(ev)
    val moduleTrees = Resolver.resolveModuleTrees(ev, modules)
    val manifests =
      moduleTrees.map(mt => (mt.module.toString(), mt.toManifest())).toMap
    manifests
  }

  implicit def millScoptEvaluatorReads[T]: EvaluatorScopt[T] =
    new mill.main.EvaluatorScopt[T]()

  lazy val millDiscover = mill.define.Discover[this.type]

}
