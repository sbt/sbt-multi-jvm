/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.sbt

import com.typesafe.sbt.multijvm.{ Jvm, JvmLogger }
import sbt.*
import Keys.*
import java.io.File
import java.lang.Boolean.getBoolean

import scala.sys.process.Process

import sbtassembly.AssemblyPlugin.assemblySettings
import sbtassembly.{ MergeStrategy, AssemblyKeys }
import sjsonnew.BasicJsonProtocol.*
import AssemblyKeys.*

object MultiJvmPlugin extends AutoPlugin {

  final case class Options(jvm: Seq[String], extra: String => Seq[String], run: String => Seq[String])

  object autoImport extends MultiJvmKeys

  import autoImport.*

  trait MultiJvmKeys {
    lazy val MultiJvm = config("multi-jvm") extend Test

    lazy val multiJvmMarker = SettingKey[String]("multi-jvm-marker")

    lazy val multiJvmTests = TaskKey[Map[String, Seq[String]]]("multi-jvm-tests")
    lazy val multiJvmTestNames = TaskKey[Seq[String]]("multi-jvm-test-names")

    lazy val multiJvmApps = TaskKey[Map[String, Seq[String]]]("multi-jvm-apps")
    lazy val multiJvmAppNames = TaskKey[Seq[String]]("multi-jvm-app-names")

    lazy val multiJvmJavaCommand = TaskKey[File]("multi-jvm-java-command")

    lazy val jvmOptions = TaskKey[Seq[String]]("jvm-options") // TODO: shouldn't that be regular `javaOptions`?
    lazy val extraOptions = SettingKey[String => Seq[String]]("extra-options")
    lazy val multiJvmCreateLogger = TaskKey[String => Logger]("multi-jvm-create-logger")

    lazy val scalatestRunner = SettingKey[String]("scalatest-runner")
    lazy val scalatestOptions = SettingKey[Seq[String]]("scalatest-options")
    lazy val scalatestClasspath = TaskKey[Classpath]("scalatest-classpath")
    lazy val scalatestScalaOptions = TaskKey[String => Seq[String]]("scalatest-scala-options")
    lazy val scalatestMultiNodeScalaOptions = TaskKey[String => Seq[String]]("scalatest-multi-node-scala-options")
    lazy val multiTestOptions = TaskKey[Options]("multi-test-options")
    lazy val multiNodeTestOptions = TaskKey[Options]("multi-node-test-options")

    lazy val appScalaOptions = TaskKey[String => Seq[String]]("app-scala-options")
    lazy val multiRunOptions = TaskKey[Options]("multi-run-options")

    lazy val multiRunCopiedClassLocation = SettingKey[File]("multi-run-copied-class-location")

    lazy val multiJvmTestJar = TaskKey[String]("multi-jvm-test-jar")
    lazy val multiJvmTestJarName = TaskKey[String]("multi-jvm-test-jar-name")

    lazy val multiNodeTest = TaskKey[Unit]("multi-node-test")
    lazy val multiNodeExecuteTests = TaskKey[Tests.Output]("multi-node-execute-tests")
    lazy val multiNodeTestOnly = InputKey[Unit]("multi-node-test-only")

    lazy val multiNodeHosts = SettingKey[Seq[String]]("multi-node-hosts")
    lazy val multiNodeHostsFileName = SettingKey[String]("multi-node-hosts-file-name")
    lazy val multiNodeProcessedHosts = TaskKey[(IndexedSeq[String], IndexedSeq[String])]("multi-node-processed-hosts")
    lazy val multiNodeTargetDirName = SettingKey[String]("multi-node-target-dir-name")
    lazy val multiNodeJavaName = SettingKey[String]("multi-node-java-name")

    // TODO fugly workaround for now
    lazy val multiNodeWorkAround =
      TaskKey[(String, (IndexedSeq[String], IndexedSeq[String]), String)]("multi-node-workaround")
  }

  @deprecated("Use MultiJvmPlugin.autoImport instead", "0.6.0")
  object MultiJvmKeys extends MultiJvmKeys

  override lazy val requires = plugins.JvmPlugin

  override lazy val projectConfigurations = Seq(MultiJvm)

  override lazy val projectSettings = multiJvmSettings

  private[this] def noTestsMessage(scoped: ScopedKey[?])(implicit display: Show[ScopedKey[?]]): String =
    "No tests to run for " + display.show(scoped)

  lazy val multiJvmSettings: Seq[Def.Setting[?]] =
    inConfig(MultiJvm)(Defaults.configSettings ++ internalMultiJvmSettings)

  // https://github.com/sbt/sbt/blob/v0.13.15/main/actions/src/main/scala/sbt/Tests.scala#L296-L298
  private[this] def showResults(log: Logger, results: Tests.Output, noTestsMessage: => String): Unit =
    TestResultLogger.Default
      .copy(printNoTests = TestResultLogger.const(_ info noTestsMessage))
      .run(log, results, "")

  private lazy val internalMultiJvmSettings = assemblySettings ++ Seq(
    multiJvmMarker := "MultiJvm",
    loadedTestFrameworks := (Test / loadedTestFrameworks).value,
    definedTests := Defaults.detectTests.value,
    multiJvmTests := collectMultiJvm(definedTests.value.map(_.name), multiJvmMarker.value),
    multiJvmTestNames := (multiJvmTests.map(_.keys.toSeq) storeAs multiJvmTestNames triggeredBy compile).value,
    multiJvmApps := collectMultiJvm(discoveredMainClasses.value, multiJvmMarker.value),
    multiJvmAppNames := (multiJvmApps.map(_.keys.toSeq) storeAs multiJvmAppNames triggeredBy compile).value,
    multiJvmJavaCommand := javaCommand(javaHome.value, "java"),
    jvmOptions := Seq.empty,
    extraOptions := { (_: String) => Seq.empty },
    multiJvmCreateLogger := { (name: String) => new JvmLogger(name) },
    scalatestRunner := "org.scalatest.tools.Runner",
    scalatestOptions := defaultScalatestOptions,
    scalatestClasspath := managedClasspath.value.filter(_.data.name.contains("scalatest")),
    multiRunCopiedClassLocation := new File(target.value, "multi-run-copied-libraries"),
    scalatestScalaOptions := scalaOptionsForScalatest(
      scalatestRunner.value,
      scalatestOptions.value,
      fullClasspath.value,
      multiRunCopiedClassLocation.value
    ),
    scalatestMultiNodeScalaOptions := scalaMultiNodeOptionsForScalatest(scalatestRunner.value, scalatestOptions.value),
    multiTestOptions := Options(jvmOptions.value, extraOptions.value, scalatestScalaOptions.value),
    multiNodeTestOptions := Options(jvmOptions.value, extraOptions.value, scalatestMultiNodeScalaOptions.value),
    appScalaOptions := scalaOptionsForApps(fullClasspath.value),
    connectInput := true,
    multiRunOptions := Options(jvmOptions.value, extraOptions.value, appScalaOptions.value),
    executeTests := multiJvmExecuteTests.value,
    testOnly := multiJvmTestOnly.evaluated,
    test := showResults(streams.value.log, executeTests.value, "No tests to run for MultiJvm"),
    run := multiJvmRun.evaluated,
    runMain := multiJvmRun.evaluated,

    // TODO try to make sure that this is only generated on a need to have basis
    multiJvmTestJar := (assembly / assemblyOutputPath).map(_.getAbsolutePath).dependsOn(assembly).value,
    multiJvmTestJarName := (assembly / assemblyOutputPath).value.getAbsolutePath,
    multiNodeTest := {
      implicit val display = Project.showContextKey(state.value)
      showResults(streams.value.log, multiNodeExecuteTests.value, noTestsMessage(resolvedScoped.value))
    },
    multiNodeExecuteTests := multiNodeExecuteTestsTask.value,
    multiNodeTestOnly := multiNodeTestOnlyTask.evaluated,
    multiNodeHosts := Seq.empty,
    multiNodeHostsFileName := "multi-node-test.hosts",
    multiNodeProcessedHosts := processMultiNodeHosts(
      multiNodeHosts.value,
      multiNodeHostsFileName.value,
      multiNodeJavaName.value,
      streams.value
    ),
    multiNodeTargetDirName := "multi-node-test",
    multiNodeJavaName := "java",
    // TODO there must be a way get at keys in the tasks that I just don't get
    multiNodeWorkAround := (multiJvmTestJar.value, multiNodeProcessedHosts.value, multiNodeTargetDirName.value),

    // here follows the assembly parts of the config
    // don't run the tests when creating the assembly
    assembly / test := {},

    // we want everything including the tests and test frameworks
    assembly / fullClasspath := (MultiJvm / fullClasspath).value,

    // the first class wins just like a classpath
    // just concatenate conflicting text files
    assembly / assemblyMergeStrategy := {
      case n if n.endsWith(".class") => MergeStrategy.first
      case n if n.endsWith(".txt")   => MergeStrategy.concat
      case n if n.endsWith("NOTICE") => MergeStrategy.concat
      case n                         => (assembly / assemblyMergeStrategy).value.apply(n)
    },
    assembly / assemblyJarName := {
      name.value + "_" + scalaVersion.value + "-" + version.value + "-multi-jvm-assembly.jar"
    }
  )

  def collectMultiJvm(discovered: Seq[String], marker: String): Map[String, Seq[String]] = {
    val found = discovered filter (_.contains(marker)) groupBy (multiName(_, marker))
    found map { case (key, values) =>
      val totalNodes = sys.props.get(marker + "." + key + ".nrOfNodes").getOrElse(values.size.toString).toInt
      val sortedClasses = values.sorted
      val totalClasses = sortedClasses.padTo(totalNodes, sortedClasses.last)
      (key, totalClasses)
    }
  }

  def multiName(name: String, marker: String): String = name.split(marker).head

  def multiSimpleName(name: String): String = name.split("\\.").last

  def javaCommand(javaHome: Option[File], name: String): File = {
    val home = javaHome.getOrElse(new File(System.getProperty("java.home")))
    new File(new File(home, "bin"), name)
  }

  lazy val defaultScalatestOptions: Seq[String] =
    if (getBoolean("sbt.log.noformat")) Seq("-oW") else Seq("-o")

  def scalaOptionsForScalatest(
      runner: String,
      options: Seq[String],
      fullClasspath: Classpath,
      multiRunCopiedClassDir: File
  ): String => Seq[String] = {
    val directoryBasedClasspathEntries = fullClasspath.files.filter(_.isDirectory)
    // Copy over just the jars to this folder.
    fullClasspath.files
      .filter(_.isFile)
      .foreach(classpathFile =>
        IO.copyFile(classpathFile, new File(multiRunCopiedClassDir, classpathFile.getName), true)
      )
    val cp =
      directoryBasedClasspathEntries.absString + File.pathSeparator + multiRunCopiedClassDir.getAbsolutePath + File.separator + "*"
    (testClass: String) => { Seq("-cp", cp, runner, "-s", testClass) ++ options }
  }

  def scalaMultiNodeOptionsForScalatest(runner: String, options: Seq[String]): String => Seq[String] = {
    (testClass: String) =>
      { Seq(runner, "-s", testClass) ++ options }
  }

  def scalaOptionsForApps(classpath: Classpath): String => Seq[String] = {
    val cp = classpath.files.absString
    (mainClass: String) => Seq("-cp", cp, mainClass)
  }

  lazy val multiJvmExecuteTests: Def.Initialize[sbt.Task[Tests.Output]] = Def.task {
    runMultiJvmTests(
      multiJvmTests.value,
      multiJvmMarker.value,
      multiJvmJavaCommand.value,
      multiTestOptions.value,
      sourceDirectory.value,
      multiJvmCreateLogger.value,
      streams.value.log
    )
  }

  lazy val multiJvmTestOnly: Def.Initialize[sbt.InputTask[Unit]] = InputTask.createDyn(
    loadForParser(multiJvmTestNames)((s, i) => Defaults.testOnlyParser(s, i getOrElse Nil))
  ) {
    Def.task { case (selection, _extraOptions) =>
      val s = streams.value
      val options = multiTestOptions.value
      val opts = options.copy(extra = (s: String) => { options.extra(s) ++ _extraOptions })
      val filters = selection.map(GlobFilter(_))
      val tests = multiJvmTests.value.filterKeys(name => filters.exists(_.accept(name)))
      Def.task {
        val results = runMultiJvmTests(
          tests,
          multiJvmMarker.value,
          multiJvmJavaCommand.value,
          opts,
          sourceDirectory.value,
          multiJvmCreateLogger.value,
          s.log
        )
        showResults(s.log, results, "No tests to run for MultiJvm")
      }
    }
  }

  def runMultiJvmTests(
      tests: Map[String, Seq[String]],
      marker: String,
      javaBin: File,
      options: Options,
      srcDir: File,
      createLogger: String => Logger,
      log: Logger
  ): Tests.Output = {
    val results =
      if (tests.isEmpty)
        List()
      else
        tests.map { case (_name, classes) =>
          multi(_name, classes, marker, javaBin, options, srcDir, input = false, createLogger, log)
        }
    Tests.Output(
      Tests.overall(results.map { case (_, testResult) => testResult }),
      Map.empty,
      results.map { case (testClass, _) => Tests.Summary("multi-jvm", testClass) }
    )
  }

  lazy val multiJvmRun: Def.Initialize[sbt.InputTask[Unit]] = InputTask.createDyn(
    loadForParser(multiJvmAppNames)((s, i) => runParser(s, i getOrElse Nil))
  ) {
    Def.task {
      val s = streams.value
      val apps = multiJvmApps.value
      val j = multiJvmJavaCommand.value
      val c = connectInput.value
      val dir = sourceDirectory.value
      val options = multiRunOptions.value
      val marker = multiJvmMarker.value
      val createLogger = multiJvmCreateLogger.value

      result => {
        val classes = apps.getOrElse(result, Seq.empty)
        Def.task {
          if (classes.isEmpty) s.log.info("No apps to run.")
          else multi(result, classes, marker, j, options, dir, c, createLogger, s.log)
        }
      }
    }
  }

  def runParser: (State, Seq[String]) => complete.Parser[String] = {
    import complete.DefaultParsers.*
    (_, appClasses) => Space ~> token(NotSpace examples appClasses.toSet)
  }

  def multi(
      name: String,
      classes: Seq[String],
      marker: String,
      javaBin: File,
      options: Options,
      srcDir: File,
      input: Boolean,
      createLogger: String => Logger,
      log: Logger
  ): (String, TestResult) = {
    log.info("* " + name)
    val classesHostsJavas = getClassesHostsJavas(classes, IndexedSeq.empty, IndexedSeq.empty, "")
    val hosts = classesHostsJavas.map { case (_, hostAndUser, _) => hostAndUser }
    val processes = classes.zipWithIndex map { case (testClass, index) =>
      val className = multiSimpleName(testClass)
      val jvmName = "JVM-" + (index + 1) + "-" + className
      val jvmLogger = createLogger(jvmName)
      val optionsFile = (srcDir ** (className + ".opts")).get.headOption
      val optionsFromFile =
        optionsFile map (IO.read(_)) map (_.trim.replace("\\n", " ").split("\\s+").toList) getOrElse Seq.empty[String]
      val multiNodeOptions = getMultiNodeCommandLineOptions(hosts, index, classes.size)
      val allJvmOptions = options.jvm ++ multiNodeOptions ++ optionsFromFile ++ options.extra(className)
      val runOptions = options.run(testClass)
      val connectInput = input && index == 0
      log.debug("Starting %s for %s" format (jvmName, testClass))
      log.debug("  with JVM options: %s" format allJvmOptions.mkString(" "))
      (testClass, Jvm.startJvm(javaBin, allJvmOptions, runOptions, jvmLogger, connectInput))
    }
    processExitCodes(name, processes, log)
  }

  def processExitCodes(name: String, processes: Seq[(String, Process)], log: Logger): (String, TestResult) = {
    val exitCodes = processes map { case (testClass, process) =>
      (testClass, process.exitValue())
    }
    val failures = exitCodes flatMap {
      case (testClass, exit) if exit > 0 => Some("Failed: " + testClass)
      case _                             => None
    }
    failures foreach (log.error(_))
    (name, if (failures.nonEmpty) TestResult.Failed else TestResult.Passed)
  }

  lazy val multiNodeExecuteTestsTask: Def.Initialize[sbt.Task[Tests.Output]] = Def.task {
    val (_jarName, (hostsAndUsers, javas), targetDir) = multiNodeWorkAround.value
    runMultiNodeTests(
      multiJvmTests.value,
      multiJvmMarker.value,
      multiNodeJavaName.value,
      multiNodeTestOptions.value,
      sourceDirectory.value,
      _jarName,
      hostsAndUsers,
      javas,
      targetDir,
      multiJvmCreateLogger.value,
      streams.value.log
    )
  }

  lazy val multiNodeTestOnlyTask: Def.Initialize[InputTask[Unit]] = InputTask.createDyn(
    loadForParser(multiJvmTestNames)((s, i) => Defaults.testOnlyParser(s, i getOrElse Nil))
  ) {
    Def.task { case (selected, _extraOptions) =>
      val options = multiNodeTestOptions.value
      val (_jarName, (hostsAndUsers, javas), targetDir) = multiNodeWorkAround.value
      val s = streams.value
      val opts = options.copy(extra = (s: String) => { options.extra(s) ++ _extraOptions })
      val tests = selected flatMap { name => multiJvmTests.value.get(name) map ((name, _)) }
      Def.task {
        val results = runMultiNodeTests(
          tests.toMap,
          multiJvmMarker.value,
          multiNodeJavaName.value,
          opts,
          sourceDirectory.value,
          _jarName,
          hostsAndUsers,
          javas,
          targetDir,
          multiJvmCreateLogger.value,
          s.log
        )
        showResults(s.log, results, "No tests to run for MultiNode")
      }
    }
  }

  def runMultiNodeTests(
      tests: Map[String, Seq[String]],
      marker: String,
      java: String,
      options: Options,
      srcDir: File,
      jarName: String,
      hostsAndUsers: IndexedSeq[String],
      javas: IndexedSeq[String],
      targetDir: String,
      createLogger: String => Logger,
      log: Logger
  ): Tests.Output = {
    val results =
      if (tests.isEmpty)
        List()
      else
        tests.map { case (_name, classes) =>
          multiNode(
            _name,
            classes,
            marker,
            java,
            options,
            srcDir,
            false,
            jarName,
            hostsAndUsers,
            javas,
            targetDir,
            createLogger,
            log
          )
        }
    Tests.Output(
      Tests.overall(results.map { case (_, testResult) => testResult }),
      Map.empty,
      results.map { case (testClass, _) => Tests.Summary("multi-jvm", testClass) }
    )
  }

  def multiNode(
      name: String,
      classes: Seq[String],
      marker: String,
      defaultJava: String,
      options: Options,
      srcDir: File,
      input: Boolean,
      testJar: String,
      hostsAndUsers: IndexedSeq[String],
      javas: IndexedSeq[String],
      targetDir: String,
      createLogger: String => Logger,
      log: Logger
  ): (String, TestResult) = {
    log.info("* " + name)
    val classesHostsJavas = getClassesHostsJavas(classes, hostsAndUsers, javas, defaultJava)
    val hostAndUsers = classesHostsJavas.map { case (_, hostAndUser, _) => hostAndUser }
    // TODO move this out, maybe to the hosts string as well?
    val syncProcesses = classesHostsJavas.map { case (testClass, hostAndUser, _) =>
      (testClass + " sync", Jvm.syncJar(testJar, hostAndUser, targetDir, log))
    }
    val (syncName, syncTestResult) = processExitCodes(name, syncProcesses, log)
    if (syncTestResult == TestResult.Passed) {
      val processes = classesHostsJavas.zipWithIndex map { case ((testClass, hostAndUser, java), index) =>
        val jvmName = "JVM-" + (index + 1)
        val jvmLogger = createLogger(jvmName)
        val className = multiSimpleName(testClass)
        val optionsFile = (srcDir ** (className + ".opts")).get.headOption
        val optionsFromFile =
          optionsFile map (IO.read(_)) map (_.trim.replace("\\n", " ").split("\\s+").toList) getOrElse Seq
            .empty[String]
        val multiNodeOptions = getMultiNodeCommandLineOptions(hostAndUsers, index, classes.size)
        val allJvmOptions = options.jvm ++ optionsFromFile ++ options.extra(className) ++ multiNodeOptions
        val runOptions = options.run(testClass)
        val connectInput = input && index == 0
        log.debug("Starting %s for %s" format (jvmName, testClass))
        log.debug("  with JVM options: %s" format allJvmOptions.mkString(" "))
        (
          testClass,
          Jvm.forkRemoteJava(
            java,
            allJvmOptions,
            runOptions,
            testJar,
            hostAndUser,
            targetDir,
            jvmLogger,
            connectInput,
            log
          )
        )
      }
      processExitCodes(name, processes, log)
    } else
      (syncName, syncTestResult)
  }

  private def padSeqOrDefaultTo(seq: IndexedSeq[String], default: String, max: Int): IndexedSeq[String] = {
    val realSeq = if (seq.isEmpty) IndexedSeq(default) else seq
    if (realSeq.size >= max)
      realSeq
    else
      (0 until (max - realSeq.size)).foldLeft(realSeq)((mySeq, pos) => mySeq :+ realSeq(pos % realSeq.size))
  }

  private def getClassesHostsJavas(
      classes: Seq[String],
      hostsAndUsers: IndexedSeq[String],
      javas: IndexedSeq[String],
      defaultJava: String
  ): IndexedSeq[(String, String, String)] = {
    val max = classes.length
    val tuple = (
      classes.toIndexedSeq,
      padSeqOrDefaultTo(hostsAndUsers, "localhost", max),
      padSeqOrDefaultTo(javas, defaultJava, max)
    )
    tuple.zipped.map { case (className: String, hostAndUser: String, _java: String) => (className, hostAndUser, _java) }
  }

  private def getMultiNodeCommandLineOptions(hosts: Seq[String], index: Int, maxNodes: Int): Seq[String] = {
    Seq(
      "-Dmultinode.max-nodes=" + maxNodes,
      "-Dmultinode.server-host=" + hosts(0).split("@").last,
      "-Dmultinode.host=" + hosts(index).split("@").last,
      "-Dmultinode.index=" + index
    )
  }

  private def processMultiNodeHosts(
      hosts: Seq[String],
      hostsFileName: String,
      defaultJava: String,
      s: Types.Id[Keys.TaskStreams]
  ): (IndexedSeq[String], IndexedSeq[String]) = {
    val hostsFile = new File(hostsFileName)
    val theHosts: IndexedSeq[String] =
      if (hosts.isEmpty) {
        if (hostsFile.exists && hostsFile.canRead) {
          s.log.info("Using hosts defined in file " + hostsFile.getAbsolutePath)
          IO.readLines(hostsFile).map(_.trim).filter(_.nonEmpty).toIndexedSeq
        } else
          hosts.toIndexedSeq
      } else {
        if (hostsFile.exists && hostsFile.canRead)
          s.log.info(
            "Hosts from setting " + multiNodeHosts.key.label + " is overriding file " + hostsFile.getAbsolutePath
          )
        hosts.toIndexedSeq
      }

    theHosts.map { x =>
      val elems = x.split(":").toList.take(2).padTo(2, defaultJava)
      (elems(0), elems(1))
    } unzip
  }
}
