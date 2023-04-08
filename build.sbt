lazy val `sbt-multi-jvm` = project in file(".")

crossScalaVersions := Seq("2.12.17", "2.10.7")
organization := "com.github.sbt"
name := "sbt-multi-jvm"
enablePlugins(SbtPlugin)
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.18"
    case "2.12" => "1.2.8" // set minimum sbt version
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

scalacOptions ++= {
  if((pluginCrossBuild / sbtVersion).value.startsWith("0.13"))
    Seq("-target:jvm-1.6")
  else
    Nil
}

// publish settings
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
