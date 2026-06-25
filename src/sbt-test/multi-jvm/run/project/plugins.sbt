addSbtPlugin(
  "com.github.sbt" % "sbt-multi-jvm" % sys.props.getOrElse("plugin.version", sys.error("plugin.version not set"))
)
