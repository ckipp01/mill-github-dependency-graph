package io.kipp.mill.github.dependency.graph

private[graph] object Discover {
  implicit def millEvaluatorTokenReader =
    mill.main.TokenReaders.millEvaluatorTokenReader
}
