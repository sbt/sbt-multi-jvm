package com.typesafe.sbt.compat

import sbt.Def

/**
 * sbt 2 caches every task by default and requires a `JsonFormat` for the task's
 * inputs/result. Tasks whose values are not serializable (functions, `Options`,
 * `Tests.Output`, classpaths, forked-JVM results) opt out via `Def.uncached`.
 */
object Caching {
  inline def uncached[A](inline a: A): A = Def.uncached(a)
}
