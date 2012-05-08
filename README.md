sbt-multi-jvm
=============

An [sbt] plugin for running scalatest tests in multiple JVMs. This plugin
requires sbt 0.11.x

[sbt]: https://github.com/harrah/xsbt


Add plugin
----------

To use the plugin in a project add the following to `project/plugins.sbt`:

    resolvers += Classpaths.typesafeResolver

    addSbtPlugin("com.typesafe.sbtmultijvm" % "sbt-multi-jvm" % "0.1.7")


More information
----------------

For more information about using sbt-multi-jvm see the
[akka documentation][akka-docs].

[akka-docs]: http://akka.io/docs/akka/snapshot/dev/multi-jvm-testing.html

License
-------
Copyright 2012 Typesafe, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.