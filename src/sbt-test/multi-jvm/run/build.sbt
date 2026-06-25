scalaVersion := "2.13.14"

// MultiJvmPlugin is an AutoPlugin: enabling it applies the MultiJvm config and
// settings, so no manual `.settings(multiJvmSettings)` splat is needed (which
// keeps this build file syntax-compatible with both sbt 1 and sbt 2).
enablePlugins(MultiJvmPlugin)

// scalatest goes in Test; MultiJvm extends Test, so it is available to the
// multi-jvm sources and the test frameworks are loaded from the Test scope.
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
