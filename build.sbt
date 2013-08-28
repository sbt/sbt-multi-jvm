
organization := "com.typesafe.sbt"

name := "sbt-multi-jvm"

version := "0.3.8-SNAPSHOT"

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
  val baseUrl = "http://scalasbt.artifactoryonline.com/scalasbt"
  val kind = if (isSnapshot.value) "snapshots" else "releases"
  val name = s"sbt-plugin-$kind"
  Some(Resolver.url(s"publish-$name", url(s"$baseUrl/$name"))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false
