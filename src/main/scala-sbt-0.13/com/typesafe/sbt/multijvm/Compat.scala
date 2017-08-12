package com.typesafe.sbt.multijvm

private[sbt] object Compat {

  type Process = sbt.Process
  val Process = sbt.Process

  object Implicits extends sbinary.DefaultProtocol {
    implicit def seqFormat[A: sbinary.Format] = sbt.Cache.seqFormat[A]
  }

  type TestResultValue = sbt.TestResult.Value

  implicit class ProcessOps(val self: sbt.ProcessBuilder) extends AnyVal {
    def lineStream: Stream[String] = self.lines_!
  }

  implicit class ShowOps[A](val self: sbt.Show[A]) extends AnyVal {
    def show(a: A) = self.apply(a)
  }

}
