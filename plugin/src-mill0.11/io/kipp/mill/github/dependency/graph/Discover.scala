package io.kipp.mill.github.dependency.graph

private[graph] object Discover {
  def apply[T] = mill.define.Discover[T]
}
