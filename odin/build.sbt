name := "benchmarks-odin"

libraryDependencies ++= {
  val procVersion   = "7.5.1"
  val scoptVersion = "4.0.0-RC2"

  Seq(
    "org.clulab"    %% "processors-main"         % procVersion,
    "org.clulab"    %% "processors-odin"         % procVersion,
    "ai.lum"        %% "common"                  % "0.0.8",
    "com.github.scopt" %% "scopt" % scoptVersion
  )
}
