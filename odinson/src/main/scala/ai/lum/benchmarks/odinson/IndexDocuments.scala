package ai.lum.benchmarks.odinson

import java.io.File

import ai.lum.benchmarks.BuildInfo

object IndexDocuments {

  case class CLIArgs(
    val documentsDir: Option[File] = None,
    val indexDest: Option[File] = None
  )

  def main(args: Array[String]): Unit = {

    val appName = "odinson-indexer"
    val parser = new scopt.OptionParser[CLIArgs](appName) {

      head(appName, s"${BuildInfo.version}")
      help("help") text ("display this message")
      version("version") text ("display version info")

      opt[File]("documents").abbr("i").valueName("/path/to/dir/of/json/files") action { (docsDir, c) =>
        c.copy(documentsDir = Some(docsDir))
      } text ("A path to a directory of org.clu.processors json documents.")

      opt[File]("out").abbr("o").valueName("/dest/of/index") action { (out, c) =>
        c.copy(indexDest = Some(out))
      } text ("The destination path for the Odinson index.")

    }

    val res: Option[CLIArgs] = parser.parse(args, CLIArgs())
    if (res.isEmpty || res.map(_.documentsDir).isEmpty || res.map(_.indexDest).isEmpty) {
      sys.exit(1)
    }
  }

}
