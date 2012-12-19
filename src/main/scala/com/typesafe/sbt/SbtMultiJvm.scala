/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.sbt

import sbt._
import Keys._
import complete.Parsers._
import complete.Parser
import Parser._
import Cache.seqFormat
import sbinary.DefaultProtocol.StringFormat
import java.io.File
import java.lang.Boolean.getBoolean
import scala.Console.{ GREEN, RESET }
import sbtassembly.Plugin._
import AssemblyKeys._
import com.typesafe.sbt.multijvm._

object SbtMultiJvm extends Plugin {
  case class RunWith(java: File, scala: ScalaInstance)
  case class Options(jvm: Seq[String], extra: String => Seq[String], scala: String => Seq[String])

  object MultiJvmKeys {
    val MultiJvm = config("multi-jvm") extend(Test)

    val multiJvmMarker = SettingKey[String]("multi-jvm-marker")

    val multiJvmTests = TaskKey[Map[String, Seq[String]]]("multi-jvm-tests")
    val multiJvmTestNames = TaskKey[Seq[String]]("multi-jvm-test-names")

    val multiJvmApps = TaskKey[Map[String, Seq[String]]]("multi-jvm-apps")
    val multiJvmAppNames = TaskKey[Seq[String]]("multi-jvm-app-names")

    val java = TaskKey[File]("java")
    val runWith = TaskKey[RunWith]("run-with")

    val jvmOptions = SettingKey[Seq[String]]("jvm-options")
    val extraOptions = SettingKey[String => Seq[String]]("extra-options")

    val scalatestRunner = SettingKey[String]("scalatest-runner")
    val scalatestOptions = SettingKey[Seq[String]]("scalatest-options")
    val scalatestClasspath = TaskKey[Classpath]("scalatest-classpath")
    val scalatestScalaOptions = TaskKey[String => Seq[String]]("scalatest-scala-options")
    val scalatestMultiNodeScalaOptions = TaskKey[String => Seq[String]]("scalatest-multi-node-scala-options")
    val multiTestOptions = TaskKey[Options]("multi-test-options")
    val multiNodeTestOptions = TaskKey[Options]("multi-node-test-options")

    val appScalaOptions = TaskKey[String => Seq[String]]("app-scala-options")
    val connectInput = SettingKey[Boolean]("connect-input")
    val multiRunOptions = TaskKey[Options]("multi-run-options")

    val multiRunCopiedClassLocation = SettingKey[File]("multi-run-copied-class-location")

    val multiJvmTestJar = TaskKey[String]("multi-jvm-test-jar")
    val multiJvmTestJarName = TaskKey[String]("multi-jvm-test-jar-name")

    val multiNodeTest = TaskKey[Unit]("multi-node-test")
    val multiNodeExecuteTests = TaskKey[(TestResult.Value, Map[String, TestResult.Value])]("multi-node-execute-tests")
    val multiNodeTestOnly = InputKey[Unit]("multi-node-test-only")

    val multiNodeHosts = SettingKey[Seq[String]]("multi-node-hosts")
    val multiNodeHostsFileName = SettingKey[String]("multi-node-hosts-file-name")
    val multiNodeProcessedHosts = TaskKey[(IndexedSeq[String], IndexedSeq[String])]("multi-node-processed-hosts")
    val multiNodeTargetDirName = SettingKey[String]("multi-node-target-dir-name")
    val multiNodeJavaName = SettingKey[String]("multi-node-java-name")

    // TODO fugly workaround for now
    val multiNodeWorkAround = TaskKey[(String, (IndexedSeq[String], IndexedSeq[String]), String)]("multi-node-workaround")
  }

  import MultiJvmKeys._

  private[this] def noTestsMessage(scoped: ScopedKey[_])(implicit display: Show[ScopedKey[_]]): String =
    "No tests to run for " + display(scoped)

  lazy val multiJvmSettings: Seq[Project.Setting[_]] = inConfig(MultiJvm)(Defaults.configSettings ++ internalMultiJvmSettings)

  private def internalMultiJvmSettings = assemblySettings ++ Seq(
    multiJvmMarker := "MultiJvm",
    loadedTestFrameworks <<= loadedTestFrameworks in Test,
    definedTests <<= Defaults.detectTests,
    multiJvmTests <<= (definedTests, multiJvmMarker) map { (d, m) => collectMultiJvm(d.map(_.name), m) },
    multiJvmTestNames <<= multiJvmTests map { _.keys.toSeq } storeAs multiJvmTestNames triggeredBy compile,
    multiJvmApps <<= (discoveredMainClasses, multiJvmMarker) map collectMultiJvm,
    multiJvmAppNames <<= multiJvmApps map { _.keys.toSeq } storeAs multiJvmAppNames triggeredBy compile,
    java <<= javaHome map { javaCommand(_, "java") },
    runWith <<= (java, scalaInstance) map RunWith,
    jvmOptions := Seq.empty,
    extraOptions := { (name: String) => Seq.empty },
    scalatestRunner := "org.scalatest.tools.Runner",
    scalatestOptions := defaultScalatestOptions,
    scalatestClasspath <<= managedClasspath map { _.filter(_.data.name.contains("scalatest")) },
    multiRunCopiedClassLocation <<= target.apply(targetFile => new File(targetFile, "multi-run-copied-libraries")),
    scalatestScalaOptions <<= (scalatestRunner, scalatestOptions, fullClasspath, multiRunCopiedClassLocation) map scalaOptionsForScalatest,
    scalatestMultiNodeScalaOptions <<= (scalatestRunner, scalatestOptions) map scalaMultiNodeOptionsForScalatest,
    multiTestOptions <<= (jvmOptions, extraOptions, scalatestScalaOptions) map Options,
    multiNodeTestOptions <<= (jvmOptions, extraOptions, scalatestMultiNodeScalaOptions) map Options,
    appScalaOptions <<= fullClasspath map scalaOptionsForApps,
    connectInput := true,
    multiRunOptions <<= (jvmOptions, extraOptions, appScalaOptions) map Options,

    executeTests <<= multiJvmExecuteTests,
    testOnly <<= multiJvmTestOnly,
    test <<= (executeTests, streams) map { (results, s) => Tests.showResults(s.log, results, "No tests to run for MultiJvm") },
    run <<= multiJvmRun,
    runMain <<= multiJvmRun,
    // TODO try to make sure that this is only generated on a need to have basis
    multiJvmTestJar <<= (assembly, outputPath in assembly) map { (task, file) => file.getAbsolutePath } ,
    multiJvmTestJarName <<= (outputPath in assembly) map { (file) => file.getAbsolutePath },
    multiNodeTest <<= (multiNodeExecuteTests, streams, resolvedScoped, state) map { (results, s, scoped, st) =>
      implicit val display = Project.showContextKey(st)
      Tests.showResults(s.log, results, noTestsMessage(scoped)) },
    multiNodeExecuteTests <<= multiNodeExecuteTestsTask,
    multiNodeTestOnly <<= multiNodeTestOnlyTask,
    multiNodeHosts := Seq.empty,
    multiNodeHostsFileName := "multi-node-test.hosts",
    multiNodeProcessedHosts <<= (multiNodeHosts, multiNodeHostsFileName, multiNodeJavaName, streams) map processMultiNodeHosts,
    multiNodeTargetDirName := "multi-node-test",
    multiNodeJavaName := "java",
    // TODO there must be a way get at keys in the tasks that I just don't get
    multiNodeWorkAround <<= (multiJvmTestJar, multiNodeProcessedHosts, multiNodeTargetDirName) map { case x => x },

    // here follows the assembly parts of the config
    // don't run the tests when creating the assembly
    test in assembly := {},
    // we want everything including the tests and test frameworks
    fullClasspath in assembly <<= fullClasspath in MultiJvm,
    // the first class wins just like a classpath
    // just concatenate conflicting text files
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        case n if n.endsWith(".class") => MergeStrategy.first
        case n if n.endsWith(".txt") => MergeStrategy.concat
        case n if n.endsWith("NOTICE") => MergeStrategy.concat
        case n => old(n)
      }
    },
    jarName in assembly <<= (name, scalaVersion, version) { _ + "_" + _ + "-" + _ + "-multi-jvm-assembly.jar" }
  )

  def collectMultiJvm(discovered: Seq[String], marker: String): Map[String, Seq[String]] = {
    val found = discovered filter (_.contains(marker)) groupBy (multiName(_, marker))
    found map {
      case (key, values) =>
        val totalNodes = sys.props.get(marker + "." + key + ".nrOfNodes").getOrElse(values.size.toString).toInt
        val sortedClasses = values.sorted
        val totalClasses = sortedClasses.padTo(totalNodes, sortedClasses.last)
        (key, totalClasses)
    }
  }

  def multiName(name: String, marker: String) = name.split(marker).head

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

  def scalaMultiNodeOptionsForScalatest(runner: String, options: Seq[String]) = {
    (testClass: String) => { Seq(runner, "-s", testClass) ++ options }
  }

  def scalaOptionsForApps(classpath: Classpath) = {
    val cp = classpath.files.absString
    (mainClass: String) => Seq("-cp", cp, mainClass)
  }

  def multiJvmExecuteTests: sbt.Project.Initialize[sbt.Task[(TestResult.Value, Map[String, TestResult.Value])]] =
    (multiJvmTests, multiJvmMarker, runWith, multiTestOptions, sourceDirectory, streams) map {
    (tests, marker, runWith, options, srcDir, s) => {
      val results =
        if (tests.isEmpty)
          List()
        else tests.map {
          case (name, classes) => multi(name, classes, marker, runWith, options, srcDir, false, s.log)
        }
      (Tests.overall(results.map(_._2)), results.toMap)
    }
  }

  def multiJvmTestOnly = InputTask(loadForParser(multiJvmTestNames)((s, i) => Defaults.testOnlyParser(s, i getOrElse Nil))) { result =>
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

  def multiJvmRun: sbt.Project.Initialize[sbt.InputTask[Unit]] = InputTask(loadForParser(multiJvmAppNames)((s, i) => runParser(s, i getOrElse Nil))) { result =>
    (result, multiJvmApps, multiJvmMarker, runWith, multiRunOptions, sourceDirectory, connectInput, multiNodeHosts, streams) map {
      (name, map, marker, runWith, options, srcDir, connect, hostsAndUsers, s) => {
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

  def multi(name: String, classes: Seq[String], marker: String, runWith: RunWith, options: Options, srcDir: File,
            input: Boolean, log: Logger): (String, TestResult.Value) = {
    val logName = "* " + name
    log.info(if (log.ansiCodesSupported) GREEN + logName + RESET else logName)
    val classesHostsJavas = getClassesHostsJavas(classes, IndexedSeq.empty, IndexedSeq.empty, "")
    val hosts = classesHostsJavas.map(_._2)
    val processes = classes.zipWithIndex map {
      case (testClass, index) => {
        val jvmName = "JVM-" + (index + 1)
        val jvmLogger = new JvmLogger(jvmName)
        val className = multiSimpleName(testClass)
        val optionsFile = (srcDir ** (className + ".opts")).get.headOption
        val optionsFromFile = optionsFile map (IO.read(_)) map (_.trim.split(" ").toList) getOrElse (Seq.empty[String])
        val multiNodeOptions = getMultiNodeCommandLineOptions(hosts, index, classes.size)
        val allJvmOptions = options.jvm ++ multiNodeOptions ++ optionsFromFile ++ options.extra(className)
        val scalaOptions = options.scala(testClass)
        val connectInput = input && index == 0
        log.debug("Starting %s for %s" format (jvmName, testClass))
        log.debug("  with JVM options: %s" format allJvmOptions.mkString(" "))
        (testClass, Jvm.startJvm(runWith.java, allJvmOptions, runWith.scala, scalaOptions, jvmLogger, connectInput))
      }
    }
    processExitCodes(name, processes, log)
  }

  def processExitCodes(name: String, processes: Seq[(String, Process)], log: Logger): (String, TestResult.Value) = {
    val exitCodes = processes map {
      case (testClass, process) => (testClass, process.exitValue)
    }
    val failures = exitCodes flatMap {
      case (testClass, exit) if exit > 0 => Some("Failed: " + testClass)
      case _ => None
    }
    failures foreach (log.error(_))
    (name, if(!failures.isEmpty) TestResult.Failed else TestResult.Passed)
  }

  def multiNodeExecuteTestsTask: sbt.Project.Initialize[sbt.Task[(TestResult.Value, Map[String, TestResult.Value])]] =
    (multiJvmTests, multiJvmMarker, multiNodeJavaName, multiNodeTestOptions, sourceDirectory, multiNodeWorkAround, streams) map {
      case (tests, marker, java, options, srcDir, (jarName, (hostsAndUsers, javas), targetDir), s) => {
        val results =
          if (tests.isEmpty)
            List()
          else tests.map {
            case (name, classes) => multiNode(name, classes, marker, java, options, srcDir, false, jarName,
              hostsAndUsers, javas, targetDir, s.log)
        }
        (Tests.overall(results.map(_._2)), results.toMap)
      }
  }

  def multiNodeTestOnlyTask = InputTask(loadForParser(multiJvmTestNames)((s, i) => Defaults.testOnlyParser(s, i getOrElse Nil))) { result =>
    (multiJvmTests, multiJvmMarker, multiNodeJavaName, multiNodeTestOptions, sourceDirectory, multiNodeWorkAround, streams, result) map {
      case (map, marker, java, options, srcDir, (jarName, (hostsAndUsers, javas), targetDir), s, (tests, extraOptions)) =>
        tests foreach { name =>
          val opts = options.copy(extra = (s: String) => { options.extra(s) ++ extraOptions })
          val classes = map.getOrElse(name, Seq.empty)
          if (classes.isEmpty) s.log.info("No tests to run.")
          else multiNode(name, classes, marker, java, opts, srcDir, false, jarName, hostsAndUsers, javas, targetDir, s.log)
        }
    }
  }

  def multiNode(name: String, classes: Seq[String], marker: String, defaultJava: String, options: Options, srcDir: File,
                input: Boolean, testJar: String, hostsAndUsers: IndexedSeq[String], javas: IndexedSeq[String], targetDir: String,
                log: Logger): (String, TestResult.Value) = {
    val logName = "* " + name
    log.info(if (log.ansiCodesSupported) GREEN + logName + RESET else logName)
    val classesHostsJavas = getClassesHostsJavas(classes, hostsAndUsers, javas, defaultJava)
    val hosts = classesHostsJavas.map(_._2)
    // TODO move this out, maybe to the hosts string as well?
    val syncProcesses = classesHostsJavas.map {
      case ((testClass, hostAndUser, java)) =>
        (testClass + " sync", Jvm.syncJar(testJar, hostAndUser, targetDir, log))
    }
    val syncResult = processExitCodes(name, syncProcesses, log)
    if (syncResult._2 == TestResult.Passed) {
      val processes = classesHostsJavas.zipWithIndex map {
        case ((testClass, hostAndUser, java), index) => {
          val jvmName = "JVM-" + (index + 1)
          val jvmLogger = new JvmLogger(jvmName)
          val className = multiSimpleName(testClass)
          val optionsFile = (srcDir ** (className + ".opts")).get.headOption
          val optionsFromFile = optionsFile map (IO.read(_)) map (_.trim.split(" ").toList) getOrElse (Seq.empty[String])
          val multiNodeOptions = getMultiNodeCommandLineOptions(hosts, index, classes.size)
          val allJvmOptions = options.jvm ++ optionsFromFile ++ options.extra(className) ++ multiNodeOptions
          val scalaOptions = options.scala(testClass)
          val connectInput = input && index == 0
          log.debug("Starting %s for %s" format (jvmName, testClass))
          log.debug("  with JVM options: %s" format allJvmOptions.mkString(" "))
          (testClass, Jvm.forkRemoteJava(java, allJvmOptions, scalaOptions, testJar, hostAndUser, targetDir,
            jvmLogger, connectInput, log))
        }
      }
      processExitCodes(name, processes, log)
    }
    else {
      syncResult
    }
  }

  private def padSeqOrDefaultTo(seq: IndexedSeq[String], default: String, max: Int): IndexedSeq[String] = {
    val realSeq = if (seq.isEmpty) IndexedSeq(default) else seq
    if (realSeq.size >= max)
      realSeq
    else
      (realSeq /: (0 until (max - realSeq.size)))((mySeq, pos) => mySeq :+ realSeq(pos % realSeq.size))
  }

  private def getClassesHostsJavas(classes: Seq[String], hostsAndUsers: IndexedSeq[String], javas: IndexedSeq[String],
                                   defaultJava: String): IndexedSeq[(String, String, String)] = {
    val max = classes.length
    (classes, padSeqOrDefaultTo(hostsAndUsers, "localhost", max), padSeqOrDefaultTo(javas, defaultJava, max)).zipped.toIndexedSeq
  }

  private def getMultiNodeCommandLineOptions(hosts: Seq[String], index: Int, maxNodes: Int): Seq[String] = {
    Seq("-Dmultinode.max-nodes=" + maxNodes, "-Dmultinode.server-host=" + hosts(0).split("@").last,
      "-Dmultinode.host=" + hosts(index).split("@").last, "-Dmultinode.index=" + index)
  }

  private def processMultiNodeHosts(hosts: Seq[String], hostsFileName: String, defaultJava: String, s: Types.Id[Keys.TaskStreams]):
    (IndexedSeq[String], IndexedSeq[String]) = {
    val hostsFile = new File(hostsFileName)
    val theHosts: IndexedSeq[String] =
      if (hosts.isEmpty) {
        if (hostsFile.exists && hostsFile.canRead) {
          s.log.info("Using hosts defined in file " + hostsFile.getAbsolutePath)
          IO.readLines(hostsFile).map(_.trim).filter(_.length > 0).toIndexedSeq
        }
        else
          hosts.toIndexedSeq
      }
      else {
        if (hostsFile.exists && hostsFile.canRead)
          s.log.info("Hosts from setting " + multiNodeHosts.key.label + " is overrriding file " + hostsFile.getAbsolutePath)
        hosts.toIndexedSeq
      }

    theHosts.map { x =>
      val elems = x.split(":").toList.take(2).padTo(2, defaultJava)
      (elems(0), elems(1))
    } unzip
  }
}
