sbtPlugin := true

organization := "com.typesafe.sbt"
name := "sbt-multi-jvm"

bintrayRepository := "sbt-plugins"
bintrayOrganization := Some("sbt-multi-jvm")

publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

versionWithGit
git.baseVersion := "0.3.12"
