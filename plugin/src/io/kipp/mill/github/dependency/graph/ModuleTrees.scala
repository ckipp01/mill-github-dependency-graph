package io.kipp.mill.github.dependency.graph

import com.github.packageurl.PackageURLBuilder
import coursier.graph.DependencyTree
import io.kipp.github.dependency.graph.domain._
import mill.scalalib.JavaModule

import scala.collection.mutable
import scala.util.Try

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
  def toFlattenedNodes()(implicit
      ctx: mill.api.Ctx
  ): Map[String, DependencyNode] = {

    // Keep track of every seen dependency and the DependencyNode for it
    val allDependencies = mutable.Map[String, DependencyNode]()
    // NOTE: maybe note necessary, but since we do this look in various times, we cache it
    val treeToName = mutable.Map[DependencyTree, String]()

    def toNode(tree: DependencyTree, root: Boolean): Unit = {

      def getNameFromTree(_tree: DependencyTree): String = {
        treeToName.getOrElseUpdate(
          _tree, {
            val _dep = _tree.dependency
            val moduleOrgName = _dep.module.orgName
            val reconciledVersion = _tree.reconciledVersion
            s"${moduleOrgName}:${reconciledVersion}"

          }
        )
      }

      val dep = tree.dependency
      val name = getNameFromTree(tree)
      val reconciledVersion = tree.reconciledVersion
      val children = tree.children
      val childrenNames = children.map(getNameFromTree)

      def putTogether: DependencyNode = {
        // TODO consider classifiers

        val purl = Try(
          PackageURLBuilder
            .aPackageURL()
            .withType("maven")
            .withNamespace(dep.module.organization.value)
            .withName(dep.module.name.value)
            .withVersion(reconciledVersion)
            .build()
        ).fold(
          e => {
            ctx.log.error(
              s"PURL can't be created from: ${dep.module.orgName}:${reconciledVersion}"
            )
            ctx.log.error(e.getMessage())
            None
          },
          validPurl => Some(validPurl.toString())
        )

        val relationShip: DependencyRelationship =
          if (root) DependencyRelationship.direct
          else DependencyRelationship.indirect

        DependencyNode(
          purl,
          // TODO we can check if original == reconciled here and add metadata that it is a reconciled version
          Map.empty,
          Some(relationShip),
          None,
          childrenNames
        )
      }

      def verifyRelationship(node: DependencyNode) =
        (root && node.isDirectDependency) || (!root && !node.isDirectDependency)

      allDependencies.get(name) match {
        // If the node is found and the relationship is correct just do nothing
        case Some(node) if verifyRelationship(node) =>
          ctx.log.debug(
            s"Already seen ${name} with this relationship in this manifest, so skipping..."
          )
        // If the node is found and the relationship is incorrect, but it's a
        // root node, then make sure to mark it as direct
        case Some(node) if root =>
          ctx.log.debug(
            s"Already seen ${name} but we're at the root level so marking as direct..."
          )
          val updated =
            node.copy(relationship = Some(DependencyRelationship.direct))
          allDependencies += ((name, updated))
        case Some(_) =>
          ctx.log.debug(
            s"Found ${name}, but it's already marked as direct so skipping..."
          )
        // Not a very elegant check, but we don't want to include a range in
        // here. These shouldn't still be a range at this point, but it is for
        // whatever reason. For now ignore it. This should be incredibly rare
        // and I believe a bug in coursier.
        case None if reconciledVersion.contains(",") =>
          ctx.log.error(
            s"""Found what I think is a range version that shouldn't be here...
                |
                |${dep.module.organization.value}:${dep.module.name.value}:${reconciledVersion}
                |
                |If you see this, report it. Skipping...
                |""".stripMargin
          )
        // Unseen dependency, create a node for it
        case None =>
          val node = putTogether
          allDependencies += ((name, node))
      }

      // If all the children are already contained in allDependencies we don't even need
      // to try and process them, we just skip it and move on.
      if (childrenNames.forall(allDependencies.contains)) {
        ctx.log.debug(
          s"short circuiting as all children of ${name} are already looked at."
        )
      } else {
        // This is a bit odd, but needed in the context of
        // https://github.com/ckipp01/mill-github-dependency-graph/issues/77
        // There can be poms that _look_ like they have cyclical dependencies
        // espeically when using classifiers. This actually seems like it might
        // be another bug in Couriser:
        // https://github.com/coursier/coursier/issues/2683
        // So, for now we filter out itself if it has itself listed as a child and we
        // also filter out any children that we've already seen.
        tree.children
          .filterNot(child =>
            child == tree ||
              allDependencies.contains(getNameFromTree(child))
          )
          .foreach(toNode(_, root = false))
      }
    }

    dependencyTrees.foreach(toNode(_, root = true))
    allDependencies.toMap
  }

  def toManifest()(implicit ctx: mill.api.Ctx) = {
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
