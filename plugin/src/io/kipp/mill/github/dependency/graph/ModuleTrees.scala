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
      val name = s"${moduleOrgName}:${dep.version}"

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

      def verifyRelationship(node: DependencyNode) =
        (root && node.isDirectDependency) || (!root && !node.isDirectDependency)

      val getsEvicted = dep.version != tree.retainedVersion
      // So the idea here is that if the retained version doesn't match the dep
      // version, we know that the dep ends up getting evicted, so we don't
      // actually need it as an entry since it won't end up on the classpath.
      // However if it's a root node, we need to ensure we later go back and
      // mark it as direct. We don't do it while iterating because we may have
      // already seen the dep that evicted it. See the evicted test for an
      // example.
      if (getsEvicted && root) {
        reconciledDirects += s"${moduleOrgName}:${tree.retainedVersion}"
      } else if (getsEvicted) {
        // If we know it's evicted here, but we're not at the root level, then
        // it doesn't matter because it's still not a direct dep.
        ()
      } else {
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
