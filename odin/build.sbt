name := "benchmarks-odin"

libraryDependencies ++= {
  val procVersion   = "7.5.1"

  Seq(
    "org.clulab"    %% "processors-main"         % procVersion,
    "org.clulab"    %% "processors-odin"         % procVersion,
    "ai.lum"        %% "common"                  % "0.0.8"
  )
}
