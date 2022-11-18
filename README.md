# Mill GitHub Dependency Graph

A [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html) plugin to
submit your dependency graph to GitHub via their [Dependency Submission
API](https://github.blog/2022-06-17-creating-comprehensive-dependency-graph-build-time-detection/).

The main benifits of doing this are:

1. Being able to see your dependency graph on GitHub in your [Insights
   tab](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/exploring-the-dependencies-of-a-repository#viewing-the-dependency-graph).
   For example you can see this
   [here](https://github.com/ckipp01/mill-github-dependency-graph/network/dependencies)
   for this plugin.
2. If enabled, Dependabot can send you
   [alerts](https://docs.github.com/en/code-security/dependabot/dependabot-alerts/viewing-and-updating-dependabot-alerts)
   about security vulnerabilities in your dependencies.

## Requirements

- Right now this plugin only supports the **Mill 0.10.x series**.
- Make sure in your repo settings the Dependency Graph feature is enabled as
    well as Dependabot Alerts if you'd like them. (Settings -> Code security and
    analysis) 

## Quick Start

The easiest way to use this plugin is with the [mill-dependency-submission](https://github.com/ckipp01/mill-dependency-submission) action. You can add this to a workflow like below:

```yml
name: github-dependency-graph

on:
  push:
    branches:
      - main

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
    - uses: ckipp01/mill-dependency-submission@v1
```

You can also just run the following command from the root of your workspace
which will create the file for you:

```sh
curl -o .github/workflows/github-dependency-graph.yml --create-dirs https://raw.githubusercontent.com/ckipp01/mill-github-dependency-graph/main/.github/workflows/github-dependency-graph.yml
```
After you submit your graph you'll be able to [view your
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
./mill --import ivy:io.chris-kipp::mill-github-dependency-graph::0.1.0 show io.kipp.mill.github.dependency.graph.Graph/generate
```

### Limitation

You'll notice when using this that a lot of dependencies aren't linked back to
the repositories where they are located, some may be wrongly linked, and much of
the information the plugin is providing (like direct vs indirect) isn't actually
displayed in the UI. Much of this is either bugs or limitations on the GitHub UI
side. You can follow some conversation on this [here](https://github.com/orgs/community/discussions/19492).
