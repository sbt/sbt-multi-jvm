lazy val `sbt-multi-jvm` = project in file(".")

sbtPlugin := true

organization := "com.typesafe.sbt"
name := "sbt-multi-jvm"

// sbt cross build
crossSbtVersions := Seq("0.13.16", "1.0.4")

// fixed in https://github.com/sbt/sbt/pull/3397 (for sbt 0.13.17)
sbtBinaryVersion in update := (sbtBinaryVersion in pluginCrossBuild).value

// dependencies
libraryDependencies += Defaults.sbtPluginExtra(
  "com.eed3si9n" % "sbt-assembly" % "0.14.10",
  (sbtBinaryVersion in pluginCrossBuild).value,
  (scalaBinaryVersion in pluginCrossBuild).value
)

// compile settings
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-encoding", "UTF-8"
)

scalacOptions ++= {
  if((sbtVersion in pluginCrossBuild).value.startsWith("0.13"))
    Seq("-target:jvm-1.6")
  else
    Nil
}

// publish settings
publishMavenStyle := false
bintrayOrganization := Some("sbt-multi-jvm")
bintrayRepository := "sbt-plugins"
bintrayPackage := "sbt-multi-jvm"
bintrayReleaseOnPublish := false
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

// release settings
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ compile"), // still no tests =(
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publish"),
  releaseStepTask(bintrayRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
