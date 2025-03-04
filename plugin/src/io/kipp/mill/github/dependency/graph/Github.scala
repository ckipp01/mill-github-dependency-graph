package io.kipp.mill.github.dependency.graph

import io.kipp.github.dependency.graph.domain.DependencySnapshot
import io.kipp.github.dependency.graph.domain.Detector
import io.kipp.github.dependency.graph.domain.Job
import io.kipp.github.dependency.graph.domain.Manifest

import java.net.URI
import java.time.Instant
import scala.util.Properties

import Writers._

/** Handles all operation of dealing with the environement when running in a
  * GitHub action and also submitting the snapshot to GitHub.
  */
object Github {

  private val url = new URI(
    s"${Env.githubApiUrl}/repos/${Env.githubRepository}/dependency-graph/snapshots"
  )

  /** Does the actual submission to the GitHub API.
    *
    * @param snapshot The full snapshot to submit.
    * @param ctx
    */
  def submit(snapshot: DependencySnapshot)(implicit ctx: mill.api.Ctx): Unit = {
    ctx.log.info("Submitting your snapshot to GitHub...")
    val payload = upickle.default.write(snapshot)
    val result = requests.post(
      url.toString,
      headers = Map(
        "Content-Type" -> "application/json",
        "Authorization" -> s"token ${Env.githubToken}"
      ),
      data = payload,
      check = false
    )

    if (result.is2xx) {
      val manifestModules = snapshot.manifests.size
      val totalDependencies =
        snapshot.manifests.values.foldLeft[Seq[String]](Seq.empty) {
          (total, manifest) =>
            total ++ manifest.resolved.keys
        }
      val totalSize = totalDependencies.size
      val uniqueSize = totalDependencies.toSet.size

      ctx.log.info(s"""Correctly submitted your snapshot to GitHub!
                      |
                      |Here are some fun stats!
                      |
                      |We submitted dependencies for ${manifestModules} modules.
                      |This was a total of ${totalSize} dependencies.
                      |${uniqueSize} were unique.
                      |""".stripMargin)
    } else if (result.statusCode == 404) {
      val msg =
        """Encountered a 404, make sure you have "Dependency Graph" enabled under "Settings -> Code Security and analysis""""
      throw new Exception(msg)
    } else if (result.statusCode == 401) {
      val msg = """Unable to correctly authenticate with GitHub.
                  |
                  |Make sure you have the correct github token set up in your env.""".stripMargin
      throw new Exception(msg)
    } else {
      val msg =
        s"""It looks like something went wrong when trying to submit your dependency graph.
            |
            |[${result.statusCode}] ${result.data}""".stripMargin
      throw new Exception(msg)
    }
  }

  /** Given manifests for the project, create a full snapshot with them.
    *
    * @param manifests All of the manifests for the project
    * @return The full DependencySnapshot to be submitted to GitHub
    */
  def snapshot(manifests: Map[String, Manifest]): DependencySnapshot =
    DependencySnapshot(
      // TODO how do we increment this? Do we query the api for the last version?
      version = 0,
      job = githubJob,
      sha = Env.githubSha,
      ref = Env.githubRef,
      detector = detector,
      metadata = Map.empty,
      manifests = manifests,
      scanned = Instant.now().toString()
    )

  private lazy val detector = Detector(
    BuildInfo.detectorName,
    BuildInfo.homepage,
    BuildInfo.version
  )

  private lazy val githubJob: Job = {
    val correlator = s"${Env.githubJobName}_${Env.githubWorkflow}"
    val id = Env.githubRunId
    val html_url =
      for {
        serverUrl <- Properties.envOrNone("$GITHUB_SERVER_URL")
        repository <- Properties.envOrNone("GITHUB_REPOSITORY")
      } yield s"$serverUrl/$repository/actions/runs/$id"
    Job(id = id, correlator = correlator, html_url = html_url)
  }

  object Env {
    lazy val githubWorkflow: String = githubCIEnv("GITHUB_WORKFLOW")
    lazy val githubJobName: String = githubCIEnv("GITHUB_JOB")
    lazy val githubRunId: String = githubCIEnv("GITHUB_RUN_ID")
    lazy val githubSha: String = githubCIEnv("GITHUB_SHA")
    lazy val githubRef: String = githubCIEnv("GITHUB_REF")
    lazy val githubApiUrl: String = githubCIEnv("GITHUB_API_URL")
    lazy val githubRepository: String = githubCIEnv("GITHUB_REPOSITORY")
    lazy val githubToken: String = githubCIEnv("GITHUB_TOKEN")

    private def githubCIEnv(
        name: String
    ): String =
      Properties.envOrNone(name).getOrElse {
        val msg = s"""|It looks like there is no "${name}" set as an env variable.
                    |
                    |Are you sure you're in a GitHubAction?
                    |
                    |If you're testing locally try to call "generate" instead of "submit" .
                    """.stripMargin
        throw new Exception(msg)
      }

  }

}
