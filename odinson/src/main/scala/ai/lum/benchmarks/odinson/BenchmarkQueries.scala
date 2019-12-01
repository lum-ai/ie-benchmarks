package ai.lum.benchmarks.odinson

import java.io.File

import ai.lum.benchmarks.BuildInfo
import ai.lum.common.ConfigUtils._
import ai.lum.common.FileUtils._
import ai.lum.odinson.ExtractorEngine
import ai.lum.odinson.compiler.QueryCompiler
import ai.lum.odinson.digraph.Vocabulary
import ai.lum.odinson.lucene.search.OdinsonIndexSearcher
import ai.lum.odinson.state.State
import ai.lum.shared.Timer._
import ai.lum.shared.FileUtils._
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.FSDirectory

object BenchmarkQueries extends LazyLogging {

  case class CLIArgs(
    indexDir: Option[File] = None,
    queriesFile: Option[File] = None,
    numIterations: Option[Int] = None,
    outDir: Option[File] = None
  )

  val config   = ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    val appName = "benchmark-odinson"
    val parser = new scopt.OptionParser[CLIArgs](appName) {

      head(appName, s"${BuildInfo.version}")
      help("help") text { "display this message" }
      version("version") text { "display version info" }

      opt[File]("index").abbr("i").valueName("/path/to/odinson/index/dir") action { case (idxDir, c) =>
        c.copy(indexDir = Some(idxDir))
      } text { "Path to an Odinson index." }

      opt[File]("queries").abbr("q").valueName("/path/to/queries/file") action { case (qf, c) =>
        c.copy(queriesFile = Some(qf))
      } text { "Path to a file of Odinson queries." }

      opt[Int]("repetitions").abbr("n").valueName("1000") action { case (n, c) =>
        c.copy(numIterations = Some(n))
      } text { "Number of extraction iterations." }

      opt[File]("out").abbr("o").valueName("/path/to/output/dir") action { case (out, c) =>
        c.copy(outDir = Some(out))
      } text { "The destination path for query benchmarks." }

    }

    val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
    if (res.isEmpty || res.map(_.indexDir).isEmpty || res.map(_.queriesFile).isEmpty || res.map(_.numIterations).isEmpty || res.map(_.outDir).isEmpty) {
      sys.exit(1)
    }

    val indexDir    = res.get.indexDir.get
    val vocabFile   = new File(indexDir, "dependenciesVocabulary.txt")
    val queriesFile = res.get.queriesFile.get
    val outDir      = res.get.outDir.get
    val outFile     = new File(outDir, s"${queriesFile.getBaseName}.tsv")

    outDir.mkdirs()


    val (ee, loadTime) = time {
      val indexReader      = DirectoryReader.open(FSDirectory.open(indexDir.toPath))
      val indexSearcher    = new OdinsonIndexSearcher(indexReader, computeTotalHits = true)
      val jdbcUrl          = config[String]("odinson.state.jdbc.url")
      val state            = new State(jdbcUrl)
      state.init()

      val compiler         = new QueryCompiler(
        config[List[String]]("odinson.compiler.allTokenFields"),
        config[String]("odinson.compiler.defaultTokenField"),
        config[String]("odinson.compiler.sentenceLengthField"),
        config[String]("odinson.compiler.dependenciesField"),
        config[String]("odinson.compiler.incomingTokenField"),
        config[String]("odinson.compiler.outgoingTokenField"),
        Vocabulary.fromDirectory(indexReader.directory()),
        config[Boolean]("odinson.compiler.normalizeQueriesToDefaultField")
      )
      compiler.setState(state)

      val parentDocIdField = config[String]("odinson.index.documentIdField")
      new ExtractorEngine(
        indexSearcher,
        compiler,
        config[String]("odinson.compiler.defaultTokenField"),
        state,
        parentDocIdField)
    }

    logger.info(s"       Index: ${indexDir.getCanonicalPath}")
    logger.info(s"Queries file: ${queriesFile.getCanonicalPath}")

    // NOTE: Currently, we're assuming this file contains a single query
    val queries = queriesFile.readString().trim
    val numDocs = ee.indexReader.numDocs()

    val results: Seq[Seq[String]] = for {
      i <- 0 until res.get.numIterations.get
    } yield {
        val (res, timeElapsed) = time {
          val q = ee.compiler.compileEventQuery(queries)
          ee.query(q, numDocs)
        }

        Seq(
          "odinson",                // name of the IE system being measured
          i.toString,               // iteration of the system
          queriesFile.getBaseName,  // IE grammar
          indexDir.getName,         // corpus dir
          loadTime.toString,        // load time (NOTE: this is calculated only once)
          res.totalHits.toString,   // num. extractions
          timeElapsed.toString      // extraction time
        )
    }

    writeTsv(results, outFile, sep = "\t")
  }
}
