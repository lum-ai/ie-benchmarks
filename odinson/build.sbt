name := "benchmarks-odinson"

libraryDependencies ++= {
  val odinsonVersion   = "0.1.0-SNAPSHOT"
  val procVer = "7.5.1"

  Seq(
    "ai.lum"      %% "odinson-core"          % odinsonVersion,
    // "ai.lum"      %% "odinson-extra"         % odinsonVersion,
    "ai.lum"      %% "common"                % "0.0.8",
    "org.clulab"  %% "processors-main"       % procVer
  )
}
