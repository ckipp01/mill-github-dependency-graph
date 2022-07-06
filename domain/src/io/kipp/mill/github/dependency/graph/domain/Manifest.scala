package io.kipp.mill.github.dependency.graph.domain

/** User-defined metadata to store domain-specific information limited to 8
  *  keys with scalar values.
  *
  * @param name The name of the manifest
  * @param file The FileInfo
  * @param metadata User-defined metadata to store domain-specific information
  * limited to 8 keys with scalar values
  * @param resolved The resolved dependnecy nodes for this manifest
  */
final case class Manifest(
    name: String,
    file: Option[FileInfo],
    metadata: Map[String, String],
    resolved: Map[String, DependencyNode]
)
