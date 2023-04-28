package io.kipp.mill.github.dependency.graph

import coursier.graph.DependencyTree
import mill._
import mill.eval.Evaluator
import mill.scalalib.Dep
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
  ): Seq[ModuleTrees] = Evaluator.evalOrThrow(evaluator) {
    javaModules.map { javaModule =>
      T.task {

        val depToDependency = javaModule.resolveCoursierDependency()
        val deps: Agg[Dep] =
          javaModule.transitiveCompileIvyDeps() ++ javaModule
            .transitiveIvyDeps()
        val repos = javaModule.repositoriesTask()
        val mapDeps = javaModule.mapDependencies()
        val custom = javaModule.resolutionCustomizer()

        val (dependencies, resolution) =
          Lib.resolveDependenciesMetadata(
            repositories = repos,
            depToDependency = depToDependency,
            deps = deps,
            mapDependencies = Some(mapDeps),
            customizer = custom,
            ctx = Some(T.log)
          )

        val trees =
          DependencyTree(resolution = resolution, roots = dependencies)

        ModuleTrees(
          javaModule,
          trees
        )
      }
    }
  }

  private[graph] def computeModules(ev: Evaluator) =
    ev.rootModule.millInternal.modules.collect { case j: JavaModule => j }
}
