sbt-multi-jvm
=============

An [sbt] plugin for running scalatest tests in multiple JVMs. This plugin
requires sbt 0.13.5 or higher

[sbt]: http://www.scala-sbt.org


Add plugin
----------

To use the plugin in a project add the following to `project/plugins.sbt`:

    addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")


More information
----------------

For more information about using sbt-multi-jvm see the
[akka documentation][akka-docs].

[akka-docs]: http://doc.akka.io/docs/akka/current/scala/multi-jvm-testing.html#multi-jvm-testing

Releasing
---------
This plugin is hosted under the `sbt-multi-jvm` organisation on bintray. When publishing you should use a bintray account which can write to this organisation, by providing the following credentials in `$HOME/.bintray/.credentials`

```
  realm = Bintray API Realm
  host = api.bintray.com
  user = USERNAME
  password = TOKEN
```

License
-------
Copyright 2012-2015 Typesafe, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
