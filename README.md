sbt-multi-jvm
=============

An [sbt] plugin for running scalatest tests in multiple JVMs. This plugin
requires sbt 0.11.0-RC1.

[sbt]: https://github.com/harrah/xsbt


Add plugin
----------

### sbt 0.11.0-RC1

To use the plugin in a project add the following to `project/plugins.sbt`:

    resolvers += Classpaths.typesafeResolver

    addSbtPlugin("com.typesafe.sbtmultijvm" % "sbt-multi-jvm" % "0.1.7")


More information
----------------

For more information about using sbt-multi-jvm see the
[akka documentation][akka-docs].

[akka-docs]: http://akka.io/docs/akka/snapshot/dev/multi-jvm-testing.html
