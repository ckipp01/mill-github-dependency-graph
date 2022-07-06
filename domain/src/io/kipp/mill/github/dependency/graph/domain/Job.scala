package io.kipp.mill.github.dependency.graph.domain

/** Job that is being submitted by the dependency snapshot
  *
  * @param id The external ID of the job
  * @param correlator Correlator provides a key that is used to group snapshots
  * submitted over time. Only the "latest" submitted snapshot for a given
  * combination of job.correlator and detector.name will be considered when
  * calculating a repository's current dependencies. Correlator should be as
  * unique as it takes to distinguish all detection runs for a given "wave" of
  * CI workflow you run. If you're using GitHub Actions, a good default value
  * for this could be the environment variables GITHUB_WORKFLOW and GITHUB_JOB
  * concatenated together. If you're using a build matrix, then you'll also
  * need to add additional key(s) to distinguish between each submission inside
  * a matrix variation.
  * @param html_url The url for the job
  */
final case class Job(
    id: String,
    correlator: String,
    html_url: Option[String]
)
