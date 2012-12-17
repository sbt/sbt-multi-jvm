
sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-multi-jvm"

version := "0.3.5-SNAPSHOT"

publishMavenStyle := false

publishTo <<= (version) { version: String =>
  val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
  val (name, u) = if (version.contains("-SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
    else ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
  Some(Resolver.url(name, url(u))(Resolver.ivyStylePatterns))
}

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.5")
