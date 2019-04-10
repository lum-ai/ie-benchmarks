package ai.lum.shared

import ai.lum.shared.Timer._
import org.scalatest.{Matchers, FlatSpec}

class TestTimer extends FlatSpec with Matchers {
  "Timer" should "return Int and nanosecond time" in {
    val res = time { (1 to 1000).sum }
    res._1.isInstanceOf[Int] should be (true)
    res._2.isInstanceOf[Long] should be (true)
    res._2 should be < 1e9.toLong
  }

  it should "return String and nanosecond time" in {
    val res = time { "Hello, world! " * 500 }
    res._1.isInstanceOf[String] should be (true)
    res._2.isInstanceOf[Long] should be (true)
    res._2 should be < 1e9.toLong
  }

  it should "return Unit and nanosecond time" in {
    val res = time { (1 to 1000).foreach(_ + 1) }
    res._1.isInstanceOf[Unit] should be (true)
    res._2.isInstanceOf[Long] should be (true)
    res._2 should be < 1e9.toLong
  }


}