package io.kipp.mill.github.dependency.graph

import mill.main.EvaluatorScopt

private[graph] object Discover {
  implicit def millScoptEvaluatorReads[A]: EvaluatorScopt[A] =
    new EvaluatorScopt[A]()

  def apply[T] = mill.define.Discover[T]
}
