package io.kipp.mill.github.dependency.graph

import coursier.graph.DependencyTree
import io.kipp.github.dependency.graph.domain._
import mill.scalalib.JavaModule

import scala.collection.mutable

/** Represents a project modules an the dependency trees that belong to it.
  *
  * @param module The module
  * @param dependencyTrees The dependency Trees belonging to the module
  * NOTE: that the roots of the trees are the direct dependencies.
  */
final case class ModuleTrees(
    module: JavaModule,
    dependencyTrees: Seq[DependencyTree]
) {

  /** Takes the dependencyTrees and flattens them to fit the model of the
    * DependencyNode that GitHub wants. They become flattened and every
    * dependency has a top level entry. Only the roots of the trees however
    * get a "direct" relationship.
    *
    * @return Mapping of the name of the dependency and the DependencyNode that
    * corresponds to it. The format of the name is org:module:version.
    */
  def toFlattenedNodes(): Map[String, DependencyNode] = {

    val allDependencies = mutable.Map[String, DependencyNode]()

    def toNode(tree: DependencyTree, root: Boolean): Unit = {
      val dep = tree.dependency
      val name = s"${dep.module.orgName}:${dep.version}"

      def putTogether: DependencyNode = {
        // TODO consider classifiers
        val packageUrl =
          s"pkg:maven/${dep.module.organization.value}/${dep.module.name.value}@${dep.version}"
        val relationShip: DependencyRelationship =
          if (root) DependencyRelationship.direct
          else DependencyRelationship.indirect
        val dependencies = tree.children.map { child =>
          s"${child.dependency.module.orgName}:${child.dependency.version}"
        }
        DependencyNode(
          Some(packageUrl),
          Map.empty,
          Some(relationShip),
          None,
          dependencies
        )
      }

      // TODO revisit this, we should just check if it exists and if the
      // relationship is correct, if so, don't overwrite it like we do now.
      val node = allDependencies.getOrElse(name, putTogether)

      // If we are still at the top level and the dep was already a transitive
      // dep, we now mark it as direct.
      val updated =
        if (root) node.copy(relationship = Some(DependencyRelationship.direct))
        else node
      allDependencies += ((name, updated))
      tree.children.foreach(toNode(_, root = false))
    }

    dependencyTrees.foreach(toNode(_, root = true))
    allDependencies.toMap
  }

  def toManifest() = {
    // There is still a bit of uncertainty here about how this should be
    // structured and we won't know until we hear back from the dependabot
    // team. Should every modules be considered a manifest, or is there just
    // one manifest with many projects. I think there should only be one, but
    // for now we copy the behavior of
    // https://github.com/scalacenter/sbt-github-dependency-graph for them to
    // be aligned.
    val name = module.toString()
    // TODO in the future we may want to also figure out how to resolve these
    // locations if they are defined in other files, but for now we just say build.sc
    val file = FileInfo("build.sc")
    val resolved = toFlattenedNodes()
    Manifest(name, Some(file), Map.empty, resolved)
  }
}
