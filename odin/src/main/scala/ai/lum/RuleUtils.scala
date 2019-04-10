package ai.lum

import scala.io.Source

/**
  * Utilities to read rule files
  * Derived from org.clulab.reach
  */
object RuleUtils {

  def readResource(filename: String): String = {
    val source = Source.fromURL(getClass.getResource(filename))
    val data = source.mkString
    source.close()
    data
  }

  def readFile(filename: String): String = {
    val source = Source.fromFile(filename)
    val data = source.mkString
    source.close()
    data
  }

  def mkRules(files: Seq[String]): Seq[(String, String)] = {
    files flatMap mkRules
  }

  def mkRules(file: String): Seq[(String, String)] = {
    Seq(file -> readFile(file))
  }
}
