package ai.lum.shared

import java.io.{File, PrintWriter}

import org.clulab.serialization.json.JSONSerializer
import org.clulab.processors.{Document => ProcessorsDocument}
import ai.lum.common.FileUtils._

object FileUtils {

  val SUPPORTED_EXTENSIONS = "(?i).*?\\.json$"

  def docsFromDir(s: String): Seq[ProcessorsDocument] = docsFromDir(new File(s))

  def docsFromDir(f: File): Seq[ProcessorsDocument] = {
    val files = f.listFilesByRegex(SUPPORTED_EXTENSIONS, recursive = true).toSeq
    files.map(deserializeDoc)
  }

  def deserializeDoc(f: File): ProcessorsDocument = f.getName.toLowerCase match {
    // any color as long as it's black
    case json if json.endsWith(".json") => JSONSerializer.toDocument(f)
    case other =>
      throw new Exception(s"Cannot deserialize ${f.getName} to org.clulab.processors.Document. Unsupported extension '$other'")
  }

  def writeTsv(strings: Seq[Seq[String]], file: String, sep: String = "\t"): Unit = {
    writeTsv(strings, new File(file), sep)
  }

  /**
    * Write a TSV file from a [[Seq]] of rows.
    */
  def writeTsv(strings: Seq[Seq[String]], file: File, sep: String): Unit = {
    if (! file.getParentFile.exists) {
      file.getParentFile.mkdirs()
    }
    val p = new PrintWriter(file)
    try {
      p.write(strings.map(_.mkString(sep)).mkString("\n"))
    }
    finally {
      p.close()
    }
  }
}