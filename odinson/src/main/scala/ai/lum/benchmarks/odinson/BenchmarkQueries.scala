package ai.lum.benchmarks.odinson

import java.io.File

import ai.lum.benchmarks.BuildInfo
import ai.lum.common.ConfigUtils._
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
    runs: Option[Int] = None,
    outDir: Option[File] = None
  )

  val config   = ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    val appName = "odinson-benchmarks"
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

      opt[Int]("runs").abbr("r").valueName("1000") action { case (numRuns, c) =>
        c.copy(runs = Some(numRuns))
      } text { "Number of extraction iterations." }

      opt[File]("out").abbr("o").valueName("/path/to/output/dir") action { case (out, c) =>
        c.copy(outDir = Some(out))
      } text { "The destination path for query benchmarks." }

    }

    val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
    if (res.isEmpty || res.map(_.indexDir).isEmpty || res.map(_.queriesFile).isEmpty || res.map(_.runs).isEmpty || res.map(_.outDir).isEmpty) {
      sys.exit(1)
    }

    val indexDir    = res.get.indexDir.get
    val vocabFile   = new File(indexDir, "dependenciesVocabulary.txt")
    val queriesFile = res.get.queriesFile.get
    val outFile     = new File(res.get.outDir.get, s"${queriesFile.getName}.tsv")

    val ee: ExtractorEngine = {
      val indexReader      = DirectoryReader.open(FSDirectory.open(indexDir.toPath))
      val indexSearcher    = new OdinsonIndexSearcher(indexReader)
      val jdbcUrl          = config[String]("state.jdbc.url")
      val state            = new State(jdbcUrl)
      state.init()

      val compiler         = new QueryCompiler(
        config[List[String]]("allTokenFields"),
        config[String]("defaultTokenField"),
        config[String]("sentenceLengthField"),
        config[String]("dependenciesField"),
        config[String]("incomingTokenField"),
        config[String]("outgoingTokenField"),
        Vocabulary.fromFile(vocabFile),
        config[Boolean]("normalizeQueriesToDefaultField")
      )
      compiler.setState(state)

      val parentDocIdField = config[String]("index.documentIdField")
      new ExtractorEngine(indexSearcher, compiler, state, parentDocIdField)
    }

    logger.info(s"       Index: ${indexDir.getCanonicalPath}")
    logger.info(s"Queries file: ${queriesFile.getCanonicalPath}")

    val bufferedSource = scala.io.Source.fromFile(queriesFile)
    val queries = for {
      line <- bufferedSource.getLines
      clean = line.trim
      if clean.nonEmpty
    } yield clean

    bufferedSource.close

    val results: Seq[Seq[String]] = for {
      run <- 0 until res.get.runs.get
    } yield {
        val (res, timeElapsed) = time {
          val queryString = queries.mkString("\n")
          ee.query(queryString)
        }
        // system, query, num. extractions, time elapsed
        Seq("odinson", queriesFile.getName, res.totalHits.toString, timeElapsed.toString)
    }

    writeTsv(results, outFile, sep = "\t")
  }



}
