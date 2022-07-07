package io.kipp.mill.github.dependency.graph

import io.kipp.mill.github.dependency.graph.domain.DependencySnapshot
import io.kipp.mill.github.dependency.graph.domain.Detector
import io.kipp.mill.github.dependency.graph.domain.Job
import io.kipp.mill.github.dependency.graph.domain.Manifest

import java.net.URL
import java.time.Instant
import scala.util.Properties

object Github {

  val url = new URL(
    s"${Env.githubApiUrl}/repos/${Env.githubRepository}/dependency-graph/snapshots"
  )

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
        // TODO restructure this so I can use ctx.log
        // ctx.log.error(msg)
        // throw new Exception(s"${name} not found as an env variable.")
        throw new Exception(msg)
      }

  }

}
