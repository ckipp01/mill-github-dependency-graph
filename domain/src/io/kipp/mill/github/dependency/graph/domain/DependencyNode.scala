package io.kipp.mill.github.dependency.graph.domain

/** Represents a single dependency.
  *
  * @param package_url Package-url (PURL) of dependency. See
  * https://github.com/package-url/purl-spec for more details.
  * @param metadata User-defined metadata to store domain-specific information
  * limited to 8 keys with scalar values.
  * @param relationship A notation of whether a dependency is requested
  * directly by this manifest or is a dependency of another dependency.
  * @param scope A notation of whether the dependency is required for the
  * primary build artifact (runtime) or is only used for development. Future
  * versions of this specification may allow for more granular scopes.
  * @param dependencies Array of package-url (PURLs) of direct child dependencies.
  */
final case class DependencyNode(
    package_url: Option[String], // TODO we could make a PURL type for this
    metadata: Map[String, String],
    relationship: Option[DependencyRelationship],
    scope: Option[DependencyScope],
    dependencies: Seq[String]
)

/** A notation of whether a dependency is requested directly
  * by this manifest, or is a dependency of another dependency.
  */
sealed trait DependencyRelationship extends Product with Serializable
object DependencyRelationship {
  case object direct extends DependencyRelationship
  case object indirect extends DependencyRelationship
}

/** A notation of whether the dependency is required for the primary
  * build artifact (runtime), or is only used for development.
  * Future versions of this specification may allow for more granular
  * scopes, like `runtime:server`, `runtime:shipped`,
  * `development:test`, `development:benchmark`.
  */
sealed trait DependencyScope extends Product with Serializable
object DependencyScope {
  case object runtime extends DependencyScope
  case object development extends DependencyScope
}
