package io.kipp.mill.github.dependency.graph

import mill._
import mill.define.ExternalModule
import mill.eval.Evaluator
import mill.main.EvaluatorScopt

object Submit extends ExternalModule { outer =>

  def submit(ev: Evaluator) = T.command {
    val modules = Resolver.computeModules(ev)
    val moduleTrees = Resolver.resolveModuleTrees(ev, modules)
  }

  implicit def millScoptEvaluatorReads[T]: EvaluatorScopt[T] =
    new mill.main.EvaluatorScopt[T]()

  lazy val millDiscover = mill.define.Discover[this.type]

}
