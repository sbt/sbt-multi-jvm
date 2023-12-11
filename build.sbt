lazy val `sbt-multi-jvm` = project in file(".")

crossScalaVersions := Seq("2.12.18")
organization := "com.github.sbt"
name := "sbt-multi-jvm"
enablePlugins(SbtPlugin)
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.9.7" // set minimum sbt version
  }
}

// dependencies
libraryDependencies += Defaults.sbtPluginExtra(
  "com.eed3si9n" % "sbt-assembly" % "1.1.0",
  (pluginCrossBuild / sbtBinaryVersion).value,
  (pluginCrossBuild / scalaBinaryVersion).value
)

// compile settings
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8"
)

// publish settings
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
