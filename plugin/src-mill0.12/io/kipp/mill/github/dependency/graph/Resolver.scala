package io.kipp.mill.github.dependency.graph

import coursier.graph.DependencyTree
import mill._
import mill.eval.Evaluator
import mill.scalalib.JavaModule
import mill.scalalib.Lib

/** Utils to help find all your modules and resolve their dependencies.
  */
object Resolver {

  /** Given an evaluator and your javaModules, use coursier to resolve all of
    * their dependencies into trees.
    *
    * @param evaluator Evaluator passed in from the command
    * @param javaModules All the JavaModules to resolve dependencies from
    * @return A collection of ModuleTrees
    */
  private[graph] def resolveModuleTrees(
      evaluator: Evaluator,
      javaModules: Seq[JavaModule]
  ): Seq[ModuleTrees] = evaluator.evalOrThrow() {
    javaModules.map { javaModule =>
      Task.Anon {

        val deps =
          javaModule.transitiveCompileIvyDeps() ++ javaModule
            .transitiveIvyDeps()
        val repos = javaModule.repositoriesTask()
        val mapDeps = javaModule.mapDependencies()
        val custom = javaModule.resolutionCustomizer()

        Lib
          .resolveDependenciesMetadataSafe(
            repositories = repos,
            deps = deps,
            mapDependencies = Some(mapDeps),
            customizer = custom,
            ctx = Some(T.log)
          )
          .map { resolution =>
            val trees =
              DependencyTree(
                resolution = resolution,
                roots = deps.map(_.dep).toSeq
              )

            ModuleTrees(
              javaModule,
              trees
            )

          }

      }
    }
  }

  private[graph] def computeModules(ev: Evaluator) =
    ev.rootModule.millInternal.modules.collect { case j: JavaModule => j }
}
