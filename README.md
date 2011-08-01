sbt-multi-jvm
=============

An [sbt] plugin for running scalatest tests in multiple JVMs. This plugin
requires sbt 0.10.1.

[sbt]: https://github.com/harrah/xsbt


Add plugin
----------

To use the plugin in a project add the following to `project/plugins/build.sbt`:

    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

    libraryDependencies += "com.typesafe.sbt-multi-jvm" %% "sbt-multi-jvm" % "0.1.3"


More information
----------------

For more information about using sbt-multi-jvm see the
[akka documentation][akka-docs].

[akka-docs]: http://akka.io/docs/akka/snapshot/dev/multi-jvm-testing.html
