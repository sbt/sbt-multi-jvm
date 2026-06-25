/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package sbt

/**
 * Compatibility shim that lives in package `sbt` so it can re-export `Tests.Output`, which became `private[sbt]` in sbt 2.
 * Plugin code outside of package `sbt` can name the test-output type through this public alias. Compiles unchanged
 * against sbt 1 (where `Tests.Output` is public) and sbt 2.
 */
object MultiJvmCompat {
  type MultiJvmTestOutput = Tests.Output

  /** `Tests.Output` and its companion are `private[sbt]` in sbt 2; expose a constructor. */
  def testOutput(
      overall: sbt.protocol.testing.TestResult,
      events: Map[String, SuiteResult],
      summaries: Iterable[Tests.Summary]
  ): MultiJvmTestOutput = Tests.Output(overall, events, summaries)
}
