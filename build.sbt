sbtPlugin := true

organization := "com.typesafe.sbt"
name := "sbt-multi-jvm"

bintrayRepository := "sbt-plugins"
bintrayOrganization := Some("sbt-multi-jvm")

publishMavenStyle := false
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

libraryDependencies += Defaults.sbtPluginExtra(
  "com.eed3si9n" % "sbt-assembly" % "0.14.5",
  (sbtBinaryVersion in pluginCrossBuild).value,
  (scalaBinaryVersion in pluginCrossBuild).value
)

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

versionWithGit
git.baseVersion := "0.3.12"
