// sbt 2 metabuild plugins (Scala 3). Referenced by their fully-resolved
// `_sbt2_3` artifact names since the metabuild always runs under sbt 2.0.0.
libraryDependencies += "com.github.sbt" % "sbt-github-actions_sbt2_3" % "0.30.0"
libraryDependencies += "com.github.sbt" % "sbt-ci-release_sbt2_3"     % "1.11.2"
libraryDependencies += "org.scalameta"  % "sbt-scalafmt_sbt2_3"       % "2.6.1"
