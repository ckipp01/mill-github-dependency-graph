package io.kipp.mill.github.dependency.graph

object Graph extends GraphModule {

  import Discover._
  lazy val millDiscover: mill.define.Discover =
    mill.define.Discover[this.type]
}
