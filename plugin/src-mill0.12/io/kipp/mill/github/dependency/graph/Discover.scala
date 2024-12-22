package io.kipp.mill.github.dependency.graph

private[graph] object Discover {
  implicit def millEvaluatorTokenReader
      : mainargs.TokensReader[mill.eval.Evaluator] =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
