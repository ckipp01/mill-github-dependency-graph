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
    val reconciledDirects = mutable.Set[String]()

    def toNode(tree: DependencyTree, root: Boolean): Unit = {
      val dep = tree.dependency
      val moduleOrgName = dep.module.orgName
      val reconciledVersion = tree.reconciledVersion
      val name = s"${moduleOrgName}:${reconciledVersion}"

      def putTogether: DependencyNode = {
        // TODO consider classifiers
        val packageUrl =
          s"pkg:maven/${dep.module.organization.value}/${dep.module.name.value}@${reconciledVersion}"
        val relationShip: DependencyRelationship =
          if (root) DependencyRelationship.direct
          else DependencyRelationship.indirect
        val dependencies = tree.children.map { child =>
          s"${child.dependency.module.orgName}:${child.reconciledVersion}"
        }
        DependencyNode(
          Some(packageUrl),
          // TODO we can check if original == reconciled here and add metadata that it is a reconciled version
          Map.empty,
          Some(relationShip),
          None,
          dependencies
        )
      }

      def verifyRelationship(node: DependencyNode) =
        (root && node.isDirectDependency) || (!root && !node.isDirectDependency)

      allDependencies.get(name) match {
        // If the node is found and the relationship is correct just do nothing
        case Some(node) if verifyRelationship(node) => ()
        // If the node is found and the relationship is incorrect, but it's a
        // root node, then make sure to mark it as direct
        case Some(node) if root =>
          val updated =
            node.copy(relationship = Some(DependencyRelationship.direct))
          allDependencies += ((name, updated))
        // Should never really happen, but it it does do nothing
        case Some(_) => ()
        // Unseen dependency, create a node for it
        case None =>
          val node = putTogether
          allDependencies += ((name, node))
      }

      tree.children.foreach(toNode(_, root = false))
    }

    dependencyTrees.foreach(toNode(_, root = true))

    // Before we return we ensure that we take care of marking any of the root
    // reconciled versions as direct.
    allDependencies.toMap.map {
      case (key, node)
          if reconciledDirects.contains(key) && !node.isDirectDependency =>
        (key, node.copy(relationship = Some(DependencyRelationship.direct)))
      case fineAsIs => identity(fineAsIs)
    }
  }

  def toManifest() = {
    // NOTE: That this may seem odd when reading the spec that we have a
    // manifest per module basically, but we did check with the GitHub team and
    // they verified the manifests that we showed them.
    val name = module.toString()
    // TODO in the future we may want to also figure out how to resolve these
    // locations if they are defined in other files, but for now we just say build.sc
    val file = FileInfo("build.sc")
    val resolved = toFlattenedNodes()
    Manifest(name, Some(file), Map.empty, resolved)
  }
}
