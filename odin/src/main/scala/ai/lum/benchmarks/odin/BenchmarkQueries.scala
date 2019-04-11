package ai.lum.benchmarks.odin

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.json4s.BuildInfo
import org.clulab.odin.{ExtractorEngine, TextBoundMention}
import ai.lum.common.FileUtils._

import ai.lum.shared.FileUtils.{deserializeDoc, writeTsv}
import ai.lum.shared.Timer.time

object BenchmarkQueries extends App with LazyLogging {
  val config = ConfigFactory.load()

  case class CLIArgs(
      grammarFile: File = new File("queries/odin/system.yml"),
      docsDir: File = new File("10K"),
      repetitions: Int = 10,
      outDir: File = new File("output/10k/odin")
  )

  val appName = "benchmark-odin"
  val parser = new scopt.OptionParser[CLIArgs](appName) {

    head(appName, s"${BuildInfo.version}")
    help("help") text "display this message"
    version("version") text "display version info"

    opt[File]('g', "grammar") action { (g, c) =>
      c.copy(grammarFile = g)
    } text "A path to a file containing an Odin grammar."
    opt[File]('d', "documents") action { (d, c) =>
      c.copy(docsDir = d)
    } text "A path to a directory with JSON-formatted Processors Documents."
    opt[Int]('n', "repetitions") action { (r, c) =>
      c.copy(repetitions = r)
    } text "Number of extraction iterations"
    opt[File]('o', "output") action { (o, c) =>
      c.copy(outDir = o)
    } text "Location to put results TSV"
  }

  val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
  if (res.isEmpty) {
    sys.exit(1)
  }

  val grammarFile: File = res.get.grammarFile
  val outDir: File = res.get.outDir
  outDir.mkdirs()

  val outFile: File = new File(outDir, s"${grammarFile.getBaseName()}.tsv")
  val rules: String = res.get.grammarFile.readString()

  val documents: Seq[File] = res.get.docsDir.listFilesByWildcard("*.json", recursive = true).toVector

  logger.info(s"${documents.length} documents")
  logger.info(s"${res.get.repetitions} runs")
  logger.info(s"Results location: ${outFile.getCanonicalPath}")

  val extractionResults: Seq[Seq[String]] = for {
    run <- 0 until res.get.repetitions
    extractorEngine = ExtractorEngine.fromRules(rules)
  } yield {
    logger.info(s"repetition ${run + 1}")

    val (loadTimes, numExtractions, extractionTimes) = documents.par.map { docName =>
      val (document, deserElapsedTime) = time { deserializeDoc(docName) }
      val (mns, extractElapsedTime) = time { extractorEngine.extractFrom(document) }
      // for benchmarking, only retain the *number* of non-TBM matches
      (deserElapsedTime, mns.count(!_.isInstanceOf[TextBoundMention]), extractElapsedTime)
    }.unzip3

    Seq(
      "odin",                           // IE system
      run.toString,                     // repetition for benchmarking the system
      res.get.grammarFile.getBaseName,  // grammar file
      res.get.docsDir.getName,          // docs directory
      loadTimes.sum.toString,           // document load time
      numExtractions.sum.toString,      // number of extractions
      extractionTimes.sum.toString      // extraction time
    )
  }

  writeTsv(extractionResults, outFile.getCanonicalPath)
}
