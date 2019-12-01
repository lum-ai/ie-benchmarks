package ai.lum.benchmarks.odinson

import java.io.File
import java.util.concurrent.TimeUnit

import ai.lum.benchmarks.BuildInfo
import ai.lum.common.ConfigUtils._
import ai.lum.common.FileUtils._
import ai.lum.odinson.OdinsonIndexWriter
import ai.lum.odinson.digraph.Vocabulary
import ai.lum.odinson.lucene.analysis.{DependencyTokenStream, NormalizedTokenStream, OdinsonTokenStream}
import ai.lum.odinson.serialization.UnsafeSerializer
import ai.lum.shared.Timer.time
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.lucene.util.BytesRef
import org.apache.lucene.document.{Document => LuceneDocument, _}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.store.FSDirectory
import org.clulab.processors.{Sentence, Document => ProcessorsDocument}
import org.clulab.serialization.json.JSONSerializer


object IndexDocuments extends LazyLogging {

  case class CLIArgs(
    documentsDir: Option[File] = None,
    indexDest: Option[File] = None
  )

  val config                       = ConfigFactory.load()
  val documentIdField              = config[String]("odinson.index.documentIdField")
  val sentenceIdField              = config[String]("odinson.index.sentenceIdField")
  val sentenceLengthField          = config[String]("odinson.index.sentenceLengthField")
  val rawTokenField                = config[String]("odinson.index.rawTokenField")
  val wordTokenField               = config[String]("odinson.index.wordTokenField")
  val normalizedTokenField         = config[String]("odinson.index.normalizedTokenField")
  val lemmaTokenField              = config[String]("odinson.index.lemmaTokenField")
  val posTagTokenField             = config[String]("odinson.index.posTagTokenField")
  val chunkTokenField              = config[String]("odinson.index.chunkTokenField")
  val entityTokenField             = config[String]("odinson.index.entityTokenField")
  val incomingTokenField           = config[String]("odinson.index.incomingTokenField")
  val outgoingTokenField           = config[String]("odinson.index.outgoingTokenField")
  val dependenciesField            = config[String]("odinson.index.dependenciesField")

  val sortedDocValuesFieldMaxSize  = config[Int]("odinson.index.sortedDocValuesFieldMaxSize")
  val maxNumberOfTokensPerSentence = config[Int]("odinson.index.maxNumberOfTokensPerSentence")

  def main(args: Array[String]): Unit = {

    val appName = "odinson-indexer"
    val parser = new scopt.OptionParser[CLIArgs](appName) {

      head(appName, s"${BuildInfo.version}")
      help("help") text { "display this message" }
      version("version") text { "display version info" }

      opt[File]("documents").abbr("i").valueName("/path/to/dir/of/json/files") action { case (docsDir, c) =>
        c.copy(documentsDir = Some(docsDir))
      } text { "A path to a directory of Odinson json documents." }

      opt[File]("out").abbr("o").valueName("/dest/of/index") action { case (out, c) =>
        c.copy(indexDest = Some(out))
      } text { "The destination path for the Odinson index." }

    }

    val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
    if (res.isEmpty || res.map(_.documentsDir).isEmpty || res.map(_.indexDest).isEmpty) {
      sys.exit(1)
    }

    val docsDir = res.get.documentsDir.get
    val indexDir = res.get.indexDest.get

    logger.info(s"Input directory: ${docsDir.getCanonicalPath}")
    logger.info(s"Index directory: ${indexDir.getCanonicalPath}")

    val dir = FSDirectory.open(res.get.indexDest.get.toPath)
    val dependenciesVocabularyFile = Vocabulary.fromDirectory(dir)
    val addToNormalizedField: Set[String] = Set(
      rawTokenField,
      wordTokenField,
      lemmaTokenField,
      posTagTokenField,
      chunkTokenField,
      entityTokenField)

    val writer = new OdinsonIndexWriter(
      dir,
      dependenciesVocabularyFile,
      documentIdField,
      sentenceIdField,
      sentenceLengthField,
      normalizedTokenField,
      addToNormalizedField,
      incomingTokenField,
      outgoingTokenField,
      sortedDocValuesFieldMaxSize,
      maxNumberOfTokensPerSentence
    )

    val (_, timeElapsed) = time {
      docsDir
        .listFilesByWildcard("*.json", recursive = true)
        .toSeq
        .par
        .foreach { f =>
          val doc = JSONSerializer.toDocument(f)
          // ensure we have a doc ID set
          if (doc.id.isEmpty) {
            val fname = f.getName.dropRight(5)
            doc.id = Some(fname)
          }
          // store sentences in Odinson index
          val block = mkDocumentBlock(doc, writer)
          writer.addDocuments(block)
        }
      writer.close()
    }
    val minutesElapsed = TimeUnit.NANOSECONDS.toMinutes(timeElapsed)
    logger.info(s"Indexed ${docsDir.listFilesByWildcard("*.json", recursive = true).size} docs in $minutesElapsed minutes.")
  }

  /** Generates a lucene document per sentence
    */
  def mkDocumentBlock(pd: ProcessorsDocument, writer: OdinsonIndexWriter): Seq[LuceneDocument] = {

    val docId = pd.id.get

    val sentenceDocs = {
      pd.sentences
        .zipWithIndex
        .flatMap {
          case (validSentence, i) if validSentence.size <= maxNumberOfTokensPerSentence =>
            Seq(mkSentenceDoc(validSentence, docId, i.toString, writer))
          case (tooLong, _) =>
            logger.warn(s"skipping sentence with ${tooLong.size} tokens")
            Nil
        }
    }

    // NOTE: we're not storing metadata for these benchmarks,
    // so we don't need to append a parent doc to this block
    sentenceDocs
  }


  /**
    * Creates a [[org.apache.lucene.document.Document]] from an [[org.clulab.processors.Sentence]].
    * @param s An annotated (tagged, parsed, etc.) sentence
    * @param docId ID for the parent document
    * @param sentId ID for sentence
    * @return
    */
  def mkSentenceDoc(
    s: Sentence,
    docId: String,
    sentId: String,
    writer: OdinsonIndexWriter
  ): LuceneDocument = {
    val sentenceDoc = new LuceneDocument
    sentenceDoc.add(new StoredField(documentIdField, docId))
    sentenceDoc.add(new StoredField(sentenceIdField, sentId))
    sentenceDoc.add(new NumericDocValuesField(sentenceLengthField, s.size.toLong))
    sentenceDoc.add(new TextField(rawTokenField, new OdinsonTokenStream(s.raw)))
    // we want to index and store the words for displaying in the shell
    sentenceDoc.add(new TextField(wordTokenField, s.words.mkString(" "), Store.YES))
    sentenceDoc.add(new TextField(normalizedTokenField, new NormalizedTokenStream(Seq(s.raw, s.words))))
    if (s.tags.isDefined) {
      sentenceDoc.add(new TextField(posTagTokenField, new OdinsonTokenStream(s.tags.get)))
    }
    if (s.lemmas.isDefined) {
      sentenceDoc.add(new TextField(lemmaTokenField, new OdinsonTokenStream(s.lemmas.get)))
    }
    if (s.entities.isDefined) {
      sentenceDoc.add(new TextField(entityTokenField, new OdinsonTokenStream(s.entities.get)))
    }
    if (s.chunks.isDefined) {
      sentenceDoc.add(new TextField(chunkTokenField, new OdinsonTokenStream(s.chunks.get)))
    }
    if (s.dependencies.isDefined) {
      val deps = s.dependencies.get
      sentenceDoc.add(new TextField(incomingTokenField, new DependencyTokenStream(deps.incomingEdges)))
      sentenceDoc.add(new TextField(outgoingTokenField, new DependencyTokenStream(deps.outgoingEdges)))
      val graph = writer.mkDirectedGraph(deps.incomingEdges, deps.outgoingEdges, deps.roots.toArray)
      val bytes = UnsafeSerializer.graphToBytes(graph)
      if (bytes.length <= sortedDocValuesFieldMaxSize) {
        sentenceDoc.add(new SortedDocValuesField(dependenciesField, new BytesRef(bytes)))
      } else {
        logger.warn(s"serialized dependencies too big for storage: ${bytes.length} > $sortedDocValuesFieldMaxSize bytes")
      }
    }

    sentenceDoc
  }

}
