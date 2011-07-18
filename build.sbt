
sbtPlugin := true

organization := "com.typesafe"

name := "sbt-multi-jvm"

version := "0.1.2"

publishMavenStyle := true

publishTo := Some("Typesafe Publish Repo" at "http://repo.typesafe.com/typesafe/maven-releases/")

credentials += Credentials(Path.userHome / ".ivy2" / "typesafe-credentials")
