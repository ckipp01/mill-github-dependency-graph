# Mill GitHub Dependency Graph

A [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html) plugin to
submit your dependency graph to GitHub via their [Dependency Submission
API](https://github.blog/2022-06-17-creating-comprehensive-dependency-graph-build-time-detection/).

## Requirements

- Right now this plugin requires **at least Mill 0.10.3**.
- Make sure in your repo settings the Dependency Graph feature is enabled as
    well as Dependabot Alerts if you'd like them. (Settings -> Code security and
    analysis) 

## Quick Start

In the future there will be a GitHub Action for this, but for now the quickest
way to get started with this at the moment is to create a workflow in your
project with the following:

```yml
name: github-dependency-graph

on:
  push:
    branches:
      - main

env:
  GITHUB_TOKEN: ${{secrets.GITHUB_TOKEN}}

jobs:
  submit-dependency-graph:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: coursier/cache-action@v6
    - uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '17'

    - name: Submit dependency graph
      run: 
        ./mill --import ivy:io.chris-kipp::mill-github-dependency-graph::0.0.11 io.kipp.mill.github.dependency.graph.Graph/submit
```

You can also just run the following command from the root of your workspace
which will create the file for you:

```sh
curl -o .github/workflows/github-dependency-graph.yml --create-dirs https://raw.githubusercontent.com/ckipp01/mill-github-dependency-graph/main/.github/workflows/github-dependency-graph.yml
```
The plugin is an [external
module](https://com-lihaoyi.github.io/mill/mill/Modules.html#_external_modules)
so you don't need to include it in your build. After you submit your graph
you'll be able to [view your
dependencies](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/exploring-the-dependencies-of-a-repository#viewing-the-dependency-graph).

## How's this work?

The general idea is that the plugin works in a few steps:

1. Gather all the modules in your build
2. Gather all direct and transitive dependencies of those modules
3. Create a tree-like structure of these dependencies. We piggy back off
   coursier for this and use its `DependencyTree` functionality.
4. We map this structure to that of a [`DependencySnapshot`](https://github.com/ckipp01/mill-github-dependency-graph/blob/main/domain/src/io/kipp/github/dependency/graph/domain/DependencySnapshot.scala), which is what GitHub understands
5. We post this data to GitHub.

You can use another available task to see what the
[`Manifest`s](https://github.com/ckipp01/mill-github-dependency-graph/blob/main/domain/src/io/kipp/github/dependency/graph/domain/Manifest.scala)
look like locally for your project, which are the main part of the
`DependencySnapshot`.


```sh
./mill --import ivy:io.chris-kipp::mill-github-dependency-graph::0.0.11 io.kipp.mill.github.dependency.graph.Graph/generate
```
