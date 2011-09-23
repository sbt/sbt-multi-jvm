package com.typesafe.sbtmultijvm

import sbt._

import java.io.File
import java.lang.{ProcessBuilder => JProcessBuilder}
import java.io.{BufferedReader, InputStream, InputStreamReader, OutputStream}

object Jvm {
  def startJvm(javaBin: File, jvmOptions: Seq[String], si: ScalaInstance, scalaOptions: Seq[String], logger: Logger, connectInput: Boolean) = {
    forkScala(javaBin, jvmOptions, si.jars, scalaOptions, logger, connectInput)
  }

  def forkScala(javaBin: File, jvmOptions: Seq[String], scalaJars: Iterable[File], arguments: Seq[String], logger: Logger, connectInput: Boolean) = {
    val scalaClasspath = scalaJars.map(_.getAbsolutePath).mkString(File.pathSeparator)
    val bootClasspath = "-Xbootclasspath/a:" + scalaClasspath
    val mainScalaClass = "scala.tools.nsc.MainGenericRunner"
    val options = jvmOptions ++ Seq(bootClasspath, mainScalaClass) ++ arguments
    forkJava(javaBin, options, logger, connectInput)
  }

  def forkJava(javaBin: File, options: Seq[String], logger: Logger, connectInput: Boolean) = {
    val java = javaBin.toString
    val command = (java :: options.toList).toArray
    val builder = new JProcessBuilder(command: _*)
    Process(builder).run(JvmIO(logger, connectInput))
  }
}

final class JvmLogger(name: String) extends BasicLogger {
  def jvm(message: String) = "[%s] %s" format (name, message)

  def log(level: Level.Value, message: => String) = System.out.synchronized {
    System.out.println(jvm(message))
  }

  def trace(t: => Throwable) = System.out.synchronized {
    val traceLevel = getTrace
    if (traceLevel >= 0) System.out.print(StackTrace.trimmed(t, traceLevel))
  }

  def success(message: => String) = log(Level.Info, message)
  def control(event: ControlEvent.Value, message: => String) = log(Level.Info, message)

  def logAll(events: Seq[LogEvent]) = System.out.synchronized { events.foreach(log) }
}

object JvmIO {
  def apply(log: Logger, connectInput: Boolean) =
    new ProcessIO(input(connectInput), processStream(log, Level.Info), processStream(log, Level.Error))

  final val BufferSize = 8192

  def processStream(log: Logger, level: Level.Value): InputStream => Unit =
    processStream(line => log.log(level, line))

  def processStream(processLine: String => Unit): InputStream => Unit = in => {
    val reader = new BufferedReader(new InputStreamReader(in))
    def process {
      val line = reader.readLine()
      if (line != null) {
        processLine(line)
        process
      }
    }
    process
  }

  def input(connectInput: Boolean): OutputStream => Unit =
    if (connectInput) connectSystemIn else ignoreOutputStream

  def connectSystemIn(out: OutputStream) = transfer(System.in, out)

  def ignoreOutputStream = (out: OutputStream) => ()

  def transfer(in: InputStream, out: OutputStream): Unit = {
    try {
      val buffer = new Array[Byte](BufferSize)
      def read {
        val byteCount = in.read(buffer)
        if (Thread.interrupted) throw new InterruptedException
        if (byteCount > 0) {
          out.write(buffer, 0, byteCount)
          out.flush()
          read
        }
      }
      read
    } catch {
      case _: InterruptedException => ()
    }
  }
}
