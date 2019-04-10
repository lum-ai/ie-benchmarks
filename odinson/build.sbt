name := "benchmarks-odinson"

libraryDependencies ++= {
  val procVersion      = "7.5.1"
  val odinsonVersion   = "0.1.0-SNAPSHOT"

  Seq(
    "org.clulab"    %% "processors-main"       % procVersion,
    "ai.lum"        %% "odinson-core"          % odinsonVersion,
    //"ai.lum"      %% "odinson-extra"         % odinsonVersion,
    "ai.lum"        %% "common"                % "0.0.8"
  )
}
