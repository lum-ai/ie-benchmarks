name := "benchmarks-odinson"

libraryDependencies ++= {
  val odinsonVersion   = "0.2.0-SNAPSHOT"

  Seq(
    "ai.lum"        %% "odinson-core"          % odinsonVersion
    //"ai.lum"      %% "odinson-extra"         % odinsonVersion,
  )
}
