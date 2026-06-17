lazy val `sbt-multi-jvm` = project in file(".")

homepage := Some(url("https://github.com/sbt/sbt-multi-jvm"))

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

lazy val scala212 = "2.12.21"
lazy val scala3 = "3.8.4" // Scala version used by the sbt 2 metabuild
// Cross-build for sbt 2 (Scala 3) and sbt 1 (Scala 2.12)
ThisBuild / crossScalaVersions := Seq(scala3, scala212)
ThisBuild / scalaVersion := scala3
organization := "com.github.sbt"
name := "sbt-multi-jvm"
enablePlugins(SbtPlugin)
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.9.7" // set minimum sbt 1 version
    case _      => "2.0.0" // sbt 2
  }
}

// dependencies
libraryDependencies += Defaults.sbtPluginExtra(
  "com.eed3si9n" % "sbt-assembly" % "2.3.1",
  (pluginCrossBuild / sbtBinaryVersion).value,
  (pluginCrossBuild / scalaBinaryVersion).value
)
// Cross-build compatibility shims (FileRef / classpath / Tests API) for sbt 1 + sbt 2
libraryDependencies += Defaults.sbtPluginExtra(
  "com.github.sbt" % "sbt2-compat" % "0.1.0",
  (pluginCrossBuild / sbtBinaryVersion).value,
  (pluginCrossBuild / scalaBinaryVersion).value
)

// compile settings
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-encoding",
  "UTF-8"
)
// Scala 2.12-only optimizer / language flags
scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12")
    List("-language:_", "-opt-inline-from:<sources>", "-opt:l:inline")
  else
    Nil
}

// scripted: expose the plugin version to test builds and show forked output
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
scriptedBufferLog := false

// publish settings
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

developers += Developer(
  "sbt-multi-jvm ",
  "Sbt Multi-JVM Contributors",
  "",
  url("https://github.com/sbt/sbt-multi-jvm/graphs/contributors")
)

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest")

// sbt 2 requires JDK 17+
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)
