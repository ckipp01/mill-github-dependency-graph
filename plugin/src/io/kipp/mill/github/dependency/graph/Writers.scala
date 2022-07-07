package io.kipp.mill.github.dependency.graph

import io.kipp.mill.github.dependency.graph.domain.DependencyRelationship
import io.kipp.mill.github.dependency.graph.domain.DependencyScope
import upickle.default.Writer

/** Writers needed for Mill to show the result of the tasks, and also to
  * serialize the dependency snapshot.
  */
object Writers {

  /** So we do this by hand for a couple reason. We could rely on a macroW for
    *  this, but we do then hit on some weird things and need a custom Option
    *  handler and a couple other things. It ends up being just as short to
    *  manually do all this, and then we avoid having to use ujson.Null as well.
    */
  implicit val manifestWriter: Writer[domain.Manifest] =
    upickle.default.writer[ujson.Obj].comap { manifest =>
      val base = ujson.Obj(
        "name" -> manifest.name,
        "metadata" -> manifest.metadata.toJsonValue,
        "resolved" -> ujson.Obj.from(
          manifest.resolved.map { case (key, dependencyNode) =>
            val dependencyNodeObject = ujson.Obj(
              "metadata" -> dependencyNode.metadata.toJsonValue,
              "dependencies" -> ujson.Arr.from(dependencyNode.dependencies)
            )
            dependencyNode.relationship.foreach {
              case DependencyRelationship.direct =>
                dependencyNodeObject.update("relationship", "direct")
              case DependencyRelationship.indirect =>
                dependencyNodeObject.update("relationship", "indirect")
            }
            dependencyNode.scope.foreach {
              case DependencyScope.development =>
                dependencyNodeObject.update("scope", "development")
              case DependencyScope.runtime =>
                dependencyNodeObject.update("scope", "runtime")
            }
            (key, dependencyNodeObject)
          }
        )
      )

      manifest.file.foreach { file =>
        base.update(
          "file",
          ujson.Obj("source_location" -> file.source_location)
        )
      }

      base
    }

  private implicit class MetadataJson(metadata: Map[String, String]) {
    def toJsonValue = {
      ujson.Obj.from(
        metadata.map { case (key, value) =>
          (key, ujson.Str(value))
        }
      )
    }
  }

}
