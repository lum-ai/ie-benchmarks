package ai.lum.benchmarks.odinson

import java.io.File

import ai.lum.benchmarks.BuildInfo

object BenchmarkQueries {

  case class CLIArgs(
    val inFile: Option[File] = None,
    val outFile: Option[File] = None
  )

  def main(args: Array[String]): Unit = {

    val appName = "odinson-benchmarks"
    val parser = new scopt.OptionParser[CLIArgs](appName) {

      head(appName, s"${BuildInfo.version}")
      help("help") text ("display this message")
      version("version") text ("display version info")

      opt[File]("queries").abbr("i").valueName("/path/to/queries/file") action { (qf, c) =>
        c.copy(inFile = Some(qf))
      } text ("A path to a file of Odinson queries.")

      opt[File]("out").abbr("o").valueName("/path/to/output/file") action { (out, c) =>
        c.copy(outFile = Some(out))
      } text ("The destination path for query benchmarks.")

    }

    val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
    if (res.isEmpty || res.map(_.inFile).isEmpty || res.map(_.outFile).isEmpty) {
      sys.exit(1)
    }
  }

}
