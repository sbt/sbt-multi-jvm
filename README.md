# sbt-multi-jvm

[![Build Status](https://github.com/sbt/sbt-multi-jvm/actions/workflows/build-test.yml/badge.svg)](https://github.com/sbt/sbt-multi-jvm/actions/workflows/build-test.yml)
[![Repository size](https://img.shields.io/github/repo-size/sbt/sbt-multi-jvm.svg?logo=git)](https://github.com/sbt/sbt-multi-jvm)

A [sbt] plugin for running scalatest tests in multiple JVMs. This plugin requires sbt 1.9.7 or higher

[sbt]: http://www.scala-sbt.org

## Add plugin

To use the plugin in a project add the following to `project/plugins.sbt`:

    addSbtPlugin("com.github.sbt" % "sbt-multi-jvm" % "0.6.0")

## More information

For more information about using sbt-multi-jvm see the [akka documentation][akka-docs].

[akka-docs]: http://doc.akka.io/docs/akka/current/scala/multi-jvm-testing.html#multi-jvm-testing

## Releasing

1. Tag the release: `git tag -s 1.2.3`
1. Push tag: `git push upstream 1.2.3`
1. GitHub action workflow does the rest: https://github.com/sbt/sbt-multi-jvm/actions/workflows/publish.yml
  - Snapshots are published to https://oss.sonatype.org/content/repositories/snapshots/com/github/sbt/sbt-multi-jvm/
  - Releases are published to https://repo1.maven.org/maven2/com/github/sbt/sbt-multi-jvm/ (takes up to 1 hour)

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
