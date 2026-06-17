/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.sbt.compat

/**
 * sbt 1 has no task caching, so `uncached` is the identity. The matching sbt 2
 * implementation in `src/main/scala-3` delegates to `Def.uncached`.
 */
object Caching {
  def uncached[A](a: A): A = a
}
