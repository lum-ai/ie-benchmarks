package ai.lum.shared

import java.io.{File, PrintWriter}

import ai.lum.common.FileUtils._
import org.clulab.processors.{Document => ProcessorsDocument, Sentence => ProcessorsSentence}
import ai.lum.odinson.{Document => OdinsonDocument, Sentence => OdinsonSentence, _}
//import org.clulab.serialization.json.JSONSerializer
import org.clulab.struct.{DirectedGraph, GraphMap}
import ai.lum.common.ConfigFactory
import ai.lum.common.ConfigUtils._

object FileUtils {

  // load field names from config
  val config = ConfigFactory.load()
  val documentIdField   = config[String]("odinson.index.documentIdField")
  val rawTokenField     = config[String]("odinson.index.rawTokenField")
  val wordTokenField    = config[String]("odinson.index.wordTokenField")
  val lemmaTokenField   = config[String]("odinson.index.lemmaTokenField")
  val posTagTokenField  = config[String]("odinson.index.posTagTokenField")
  val chunkTokenField   = config[String]("odinson.index.chunkTokenField")
  val entityTokenField  = config[String]("odinson.index.entityTokenField")
  val dependenciesField = config[String]("odinson.index.dependenciesField")

  val SUPPORTED_EXTENSIONS = "(?i).*?\\.json$"

  def docsFromDir(s: String): Seq[OdinsonDocument] = docsFromDir(new File(s))

  def docsFromDir(f: File): Seq[OdinsonDocument] = {
    val files = f.listFilesByRegex(SUPPORTED_EXTENSIONS, recursive = true).toSeq
    files.par.map(deserializeDoc).seq
  }

  def listFiles(s: String): Seq[File] = listFiles(new File(s))

  def listFiles(f: File): Seq[File] = {
    f.listFilesByRegex(SUPPORTED_EXTENSIONS, recursive = true).toSeq
  }

  def deserializeDoc(f: File): OdinsonDocument = f.getName.toLowerCase match {
    // any color so long as it is black
    case json if json.endsWith(".json") =>
      OdinsonDocument.fromJson(f)
    case other =>
      throw new Exception(s"Cannot deserialize ${f.getName}. Unsupported extension '$other'")
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

  /** convert odinson document to processors document */
  def convertDocument(d: OdinsonDocument): ProcessorsDocument = {
    val sentences = mkSentences(d)
    new ProcessorsDocument(sentences.toArray)
  }

  /** make sequence of processors documents from odinson document */
  def mkSentences(d: OdinsonDocument): Seq[ProcessorsSentence] = {
    d.sentences.map(convertSentence)
  }

  def getByTokenField(s: OdinsonSentence, fieldName: String): Option[Array[String]] =
    s.fields.find(_.name == fieldName).map(_.asInstanceOf[TokensField].tokens.toArray)

  /** Given an array of raw tokens, produce startOffsets and endOffsets (assuming 1 space between) */
  def getOffsets(tokens: Array[String]): (Array[Int], Array[Int]) = {
    tokens.foldLeft(List[(Int, Int)]()){ (r,c) =>
      // startOffset = last endOffset + 1
      val startOffset = if(r.isEmpty) 0 else r.head._2 + 1
      // endOffset = this startOffset + word length
      (startOffset, startOffset + c.length) :: r
    }.reverse.toArray.unzip
  }

  /** convert processors sentence to odinson sentence */
  def convertSentence(s: OdinsonSentence): ProcessorsSentence = {
    val raw = getByTokenField(s, rawTokenField).get
    val (startOffset, endOffset) = getOffsets(raw)
    val word = getByTokenField(s, wordTokenField).get
    val tag = getByTokenField(s, posTagTokenField)
    val lemma = getByTokenField(s, lemmaTokenField)
    val chunk = getByTokenField(s, chunkTokenField)
    val entity = getByTokenField(s, entityTokenField)

    val graphField = s.fields.find(_.name == dependenciesField).map(_.asInstanceOf[GraphField]).get
    val edges = DirectedGraph.triplesToEdges[String](graphField.edges.toList)
    val directedGraph = DirectedGraph(edges, graphField.roots)
    val graph = GraphMap(Map("universal-enhanced" -> directedGraph))

    ProcessorsSentence(raw, startOffset, endOffset, word, tag, lemma, entity, None, chunk, None, graph, None)
  }
}