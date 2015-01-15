import bintray.Keys._

sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-multi-jvm"

version := "0.3.9-SNAPSHOT"

publishMavenStyle := false

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := Some("sbt")

bintrayPublishSettings

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")
