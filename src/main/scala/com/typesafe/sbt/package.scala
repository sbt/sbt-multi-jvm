/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe

package object sbt {

  // For backwards compatibility
  @deprecated("Use MultiJvm instead", "0.6.0")
  val SbtMultiJvm = MultiJvmPlugin
}
