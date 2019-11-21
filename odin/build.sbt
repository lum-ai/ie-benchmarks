name := "benchmarks-odin"

libraryDependencies ++= {
  val procVersion = "7.5.4"

  Seq(
    "org.clulab"       %% "processors-main"      % procVersion,
    "org.clulab"       %% "processors-odin"      % procVersion,
    "ai.lum"           %% "common"               % "0.0.8",
    "ai.lum"           %% "odinson"              % "0.2.0-SNAPSHOT",
    "com.github.scopt" %% "scopt"                % "4.0.0-RC2"
  )
}
