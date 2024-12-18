package io.kipp.mill.github.dependency.graph

import scala.annotation.nowarn

// In here for the Discover import
@nowarn("msg=Unused import")
object Graph extends GraphModule {

  import Discover._
  lazy val millDiscover: mill.define.Discover[this.type] =
    mill.define.Discover[this.type]
}
