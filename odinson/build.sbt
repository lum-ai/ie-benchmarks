name := "benchmarks-odinson"

libraryDependencies ++= {
  val odinsonVersion   = "0.1.0-SNAPSHOT"

  Seq(
    "ai.lum"        %% "odinson-core"          % odinsonVersion,
    //"ai.lum"      %% "odinson-extra"         % odinsonVersion,
  )
}
