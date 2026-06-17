/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://www.typesafe.com>
 */

import org.scalatest.wordspec.AnyWordSpec

// Each node writes a marker file when its test body actually runs, so the
// scripted `test` script can assert the JVMs were really forked and executed
// (not merely that `MultiJvm/test` returned success with nothing to run).
object Markers {
  def touch(name: String): Unit = {
    val f = new java.io.File("target/" + name)
    f.getParentFile.mkdirs()
    f.createNewFile()
  }
}

class SampleMultiJvmNode1 extends AnyWordSpec {
  "node1" should {
    "run" in { Markers.touch("node1.ran") }
  }
}

class SampleMultiJvmNode2 extends AnyWordSpec {
  "node2" should {
    "run" in { Markers.touch("node2.ran") }
  }
}
