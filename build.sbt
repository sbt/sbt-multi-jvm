
sbtPlugin := true

organization := "com.typesafe.sbtmultijvm"

name := "sbt-multi-jvm"

version := "0.2.0-M4"

publishMavenStyle := false

publishTo := Option(Classpaths.typesafeResolver)

resolvers += Resolver.url("sbt-plugin-releases",
               new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.3")

// Cross building isn't really working for the plugin dependency
//CrossBuilding.crossSbtVersions := List("0.11.2", "0.11.3")
