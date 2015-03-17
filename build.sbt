import bintray.Keys._

sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-multi-jvm"

version := "0.3.9"

bintraySettings

bintrayPublishSettings

bintrayOrganization := Some("sbt")

publishMavenStyle := false

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
