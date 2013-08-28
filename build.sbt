
organization := "com.typesafe.sbt"

name := "sbt-multi-jvm"

version := "0.3.7-SNAPSHOT"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.2")

sbtPlugin := true

publishTo := {
  import Classpaths._
  val repo = if (isSnapshot.value) sbtPluginSnapshots else sbtPluginReleases
  Some(repo)
}

publishMavenStyle := false
