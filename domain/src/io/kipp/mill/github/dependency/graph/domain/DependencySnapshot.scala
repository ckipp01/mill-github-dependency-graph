package io.kipp.mill.github.dependency.graph.domain

/** Dependency submission snapshot for GitHub.
  * Modeled after the info found in:
  *     https://docs.github.com/en/rest/dependency-graph/dependency-submission#create-a-snapshot-of-dependencies-for-a-repository
  * @param version The version of the repository snapshot submission
  * @param job The Job being sumbitted
  * @param sha The commit SHA associated with this dependency snapshot
  * @param ref The repository branch that triggered this snapshot
  * @param detector A description of the detector used.
  * @param metadata User-defined metadata to store domain-specific information
  * limited to 8 keys with scalar values
  * @param manifests A collection of package manifests
  * @param scanned  The time at which the snapshot was scanned in ISO8601Date
  */
final case class DependencySnapshot(
    version: Int,
    job: Job,
    sha: String,
    ref: String,
    detector: Detector,
    metadata: Map[
      String,
      String
    ],
    manifests: Map[String, Manifest],
    scanned: String
)
