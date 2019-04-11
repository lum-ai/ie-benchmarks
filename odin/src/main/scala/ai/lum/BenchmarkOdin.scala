package ai.lum

import ai.lum.RuleUtils._
import ai.lum.shared.Timer._
import ai.lum.shared.FileUtils._
import org.clulab.processors.{Document => ProcessorsDocument}
import org.clulab.odin.{EventMention, ExtractorEngine, TextBoundMention}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.json4s.BuildInfo

object BenchmarkOdin extends App with LazyLogging {
  val config = ConfigFactory.load()

  case class CLIArgs(
      grammarLoc: String = System.getProperty("user.home") + "/code/lum-ai/ie-benchmarks/odin/src/main/resources/ai/lum/grammar/system.yml",
      docsDir: String = "/data/nlp/corpora/lum-ai/ie-benchmarks/10K",
      runs: Int = 10,
      runDataLoc: String = "/data/nlp/corpora/lum-ai/ie-benchmarks/system.tsv"
  )

  val appName = "BenchmarkOdin"
  val parser = new scopt.OptionParser[CLIArgs](appName) {

    head(appName, s"${BuildInfo.version}")
    help("help") text "display this message"
    version("version") text "display version info"

    opt[String]('g', "grammar") action { (g, c) =>
      c.copy(grammarLoc = g)
    } text "A path to a file containing an Odin grammar."
    opt[String]('d', "documents") action { (d, c) =>
      c.copy(docsDir = d)
    } text "A path to a directory with JSON-formatted Processors Documents."
    opt[Int]('r', "runs") action { (r, c) =>
      c.copy(runs = r)
    } text "Number of extraction iterations"
    opt[String]('o', "output") action { (o, c) =>
      c.copy(runDataLoc = o)
    } text "Location to put results TSV"
  }

  val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
  if (res.isEmpty) {
    sys.exit(1)
  }
  val ruleSets: Seq[(String, String)] = mkRules(res.get.grammarLoc)

  val documents: Seq[ProcessorsDocument] = docsFromDir(res.get.docsDir)

  logger.info(s"${documents.length} documents")
  logger.info(s"${res.get.runs} runs")
  logger.info(s"Writing to ${res.get.runDataLoc}")

  val extractionResults: Seq[Seq[String]] = for {
    (ruleName, ruleSet) <- ruleSets
    extractorEngine = ExtractorEngine.fromRules(ruleSet)
    run <- 0 until res.get.runs
  } yield {
    val (extractions, timeElapsed) = time { documents map extractorEngine.extractFrom }

//    if(run == 0) {
//      println(extractions.flatMap(_.filter(! _.isInstanceOf[TextBoundMention]).map(_.text)).mkString("\n"))
//    }

    // extractor, ruleset, # documents # extractions, time elapsed
    Seq(
      "odin",
      ruleName,
      documents.length.toString,
      extractions.map(_.length).sum.toString,
      timeElapsed.toString
    )
  }

  writeTsv(extractionResults, res.get.runDataLoc)
}

