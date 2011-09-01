import sbt._
import Keys._
import Cache.seqFormat
import sbinary.DefaultProtocol.StringFormat
import java.io.File
import java.lang.Boolean.getBoolean
import scala.Console.{ GREEN, RESET }

object MultiJvmPlugin {
  case class RunWith(java: File, scala: ScalaInstance)
  case class Options(jvm: Seq[String], extra: String => Seq[String], scala: String => Seq[String])

  val MultiJvm = config("multi-jvm") extend(Test)

  val multiJvmMarker = SettingKey[String]("multi-jvm-marker")

  val multiJvmTests = TaskKey[Map[String, Seq[String]]]("multi-jvm-tests")
  val multiJvmTestNames = TaskKey[Seq[String]]("multi-jvm-test-names")

  val multiJvmApps = TaskKey[Map[String, Seq[String]]]("multi-jvm-apps")
  val multiJvmAppNames = TaskKey[Seq[String]]("multi-jvm-app-names")

  val java = SettingKey[File]("java")
  val runWith = SettingKey[RunWith]("run-with")

  val jvmOptions = SettingKey[Seq[String]]("jvm-options")
  val extraOptions = SettingKey[String => Seq[String]]("extra-options")

  val scalatestRunner = SettingKey[String]("scalatest-runner")
  val scalatestOptions = SettingKey[Seq[String]]("scalatest-options")
  val scalatestClasspath = TaskKey[Classpath]("scalatest-classpath")
  val scalatestScalaOptions = TaskKey[String => Seq[String]]("scalatest-scala-options")
  val multiTestOptions = TaskKey[Options]("multi-test-options")

  val appScalaOptions = TaskKey[String => Seq[String]]("app-scala-options")
  val connectInput = SettingKey[Boolean]("connect-input")
  val multiRunOptions = TaskKey[Options]("multi-run-options")

  val multiRunCopiedClassLocation = SettingKey[File]("multi-run-copied-class-location")

  lazy val settings: Seq[Setting[_]] = inConfig(MultiJvm)(Defaults.configSettings ++ multiJvmSettings)

  def multiJvmSettings = Seq(
    multiJvmMarker := "MultiJvm",
    loadedTestFrameworks <<= (loadedTestFrameworks in Test).identity,
    definedTests <<= Defaults.detectTests,
    multiJvmTests <<= (definedTests, multiJvmMarker) map { (d, m) => collectMultiJvm(d.map(_.name), m) },
    multiJvmTestNames <<= TaskData.write(multiJvmTests map { _.keys.toSeq }) triggeredBy compile,
    multiJvmApps <<= (discoveredMainClasses, multiJvmMarker) map collectMultiJvm,
    multiJvmAppNames <<= TaskData.write(multiJvmApps map { _.keys.toSeq }) triggeredBy compile,
    java <<= javaHome { javaCommand(_, "java") },
    runWith <<= (java, scalaInstance) apply RunWith,
    jvmOptions := Seq.empty,
    extraOptions := { (name: String) => Seq.empty },
    scalatestRunner := "org.scalatest.tools.Runner",
    scalatestOptions := defaultScalatestOptions,
    scalatestClasspath <<= managedClasspath map { _.filter(_.data.name.contains("scalatest")) },
    multiRunCopiedClassLocation <<= target.apply(targetFile => new File(targetFile, "multi-run-copied-libraries")),
    scalatestScalaOptions <<= (scalatestRunner, scalatestOptions, fullClasspath, multiRunCopiedClassLocation) map scalaOptionsForScalatest,
    multiTestOptions <<= (jvmOptions, extraOptions, scalatestScalaOptions) map Options,
    appScalaOptions <<= fullClasspath map scalaOptionsForApps,
    connectInput := true,
    multiRunOptions <<= (jvmOptions, extraOptions, appScalaOptions) map Options,
    test <<= multiJvmTest,
    testOnly <<= multiJvmTestOnly,
    run <<= multiJvmRun,
    runMain <<= multiJvmRun
  )

  def collectMultiJvm(discovered: Seq[String], marker: String): Map[String, Seq[String]] = {
    discovered filter (_.contains(marker)) groupBy (multiName(_, marker))
  }

  def multiName(name: String, marker: String) = name.split(marker).head

  def multiIdentifier(name: String, marker: String) = name.split(marker).last

  def multiSimpleName(name: String) = name.split("\\.").last

  def javaCommand(javaHome: Option[File], name: String): File = {
    val home = javaHome.getOrElse(new File(System.getProperty("java.home")))
    new File(new File(home, "bin"), name)
  }

  def defaultScalatestOptions: Seq[String] = {
    if (getBoolean("sbt.log.noformat")) Seq("-oW") else Seq("-o")
  }

  def scalaOptionsForScalatest(runner: String, options: Seq[String], fullClasspath: Classpath, multiRunCopiedClassDir: File) = {
    val directoryBasedClasspathEntries = fullClasspath.files.filter(_.isDirectory)
    // Copy over just the jars to this folder.
    fullClasspath.files.filter(_.isFile).foreach(classpathFile => IO.copyFile(classpathFile, new File(multiRunCopiedClassDir, classpathFile.getName), true))
    val cp = directoryBasedClasspathEntries.absString + File.pathSeparator + multiRunCopiedClassDir.getAbsolutePath + File.separator + "*"
    (testClass: String) => { Seq("-cp", cp, runner, "-s", testClass) ++ options }
  }

  def scalaOptionsForApps(classpath: Classpath) = {
    val cp = classpath.files.absString
    (mainClass: String) => Seq("-cp", cp, mainClass)
  }

  def multiJvmTest = (multiJvmTests, multiJvmMarker, runWith, multiTestOptions, sourceDirectory, streams) map {
    (tests, marker, runWith, options, srcDir, s) => {
      if (tests.isEmpty) s.log.info("No tests to run.")
      else tests.foreach {
        case (name, classes) => multi(name, classes, marker, runWith, options, srcDir, false, s.log)
      }
    }
  }

  def multiJvmTestOnly = InputTask(TaskData(multiJvmTestNames)(Defaults.testOnlyParser)(Nil)) { result =>
    (multiJvmTests, multiJvmMarker, runWith, multiTestOptions, sourceDirectory, streams, result) map {
      case (map, marker, runWith, options, srcDir, s, (tests, extraOptions)) =>
        tests foreach { name =>
          val opts = options.copy(extra = (s: String) => { options.extra(s) ++ extraOptions })
          val classes = map.getOrElse(name, Seq.empty)
          if (classes.isEmpty) s.log.info("No tests to run.")
          else multi(name, classes, marker, runWith, opts, srcDir, false, s.log)
        }
    }
  }

  def multiJvmRun = InputTask(TaskData(multiJvmAppNames)(runParser)(Nil)) { result =>
    (result, multiJvmApps, multiJvmMarker, runWith, multiRunOptions, sourceDirectory, connectInput, streams) map {
      (name, map, marker, runWith, options, srcDir, connect, s) => {
        val classes = map.getOrElse(name, Seq.empty)
        if (classes.isEmpty) s.log.info("No apps to run.")
        else multi(name, classes, marker, runWith, options, srcDir, connect, s.log)
      }
    }
  }

  def runParser: (State, Seq[String]) => complete.Parser[String] = {
    import complete.DefaultParsers._
    (state, appClasses) => Space ~> token(NotSpace examples appClasses.toSet)
  }

  def multi(name: String, classes: Seq[String], marker: String, runWith: RunWith, options: Options, srcDir: File, input: Boolean, log: Logger): Unit = {
    val logName = "* " + name
    log.info(if (log.ansiCodesSupported) GREEN + logName + RESET else logName)
    val processes = classes.zipWithIndex map {
      case (testClass, index) => {
        val jvmName = "JVM-" + multiIdentifier(testClass, marker)
        val jvmLogger = new JvmLogger(jvmName)
        val className = multiSimpleName(testClass)
        val optionsFile = (srcDir ** (className + ".opts")).get.headOption
        val optionsFromFile = optionsFile map (IO.read(_)) map (_.trim.split(" ").toList) getOrElse (Seq.empty[String])
        val allJvmOptions = options.jvm ++ optionsFromFile ++ options.extra(className)
        val scalaOptions = options.scala(testClass)
        val connectInput = input && index == 0
        log.debug("Starting %s for %s" format (jvmName, testClass))
        log.debug("  with JVM options: %s" format allJvmOptions.mkString(" "))
        (testClass, Jvm.startJvm(runWith.java, allJvmOptions, runWith.scala, scalaOptions, jvmLogger, connectInput))
      }
    }
    val exitCodes = processes map {
      case (testClass, process) => (testClass, process.exitValue)
    }
    val failures = exitCodes flatMap {
      case (testClass, exit) if exit > 0 => Some("Failed: " + testClass)
      case _ => None
    }
    failures foreach (log.error(_))
    if (!failures.isEmpty) error("Some processes failed")
  }
}
