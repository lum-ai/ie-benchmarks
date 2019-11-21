name := "benchmarks-shared"

libraryDependencies ++= {
  val procVersion   = "7.5.4"
  val odinsonVersion = "0.2.0-SNAPSHOT"

  Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.clulab"    %% "processors-main"     % procVersion,
    "ai.lum"        %% "common" % "0.0.8",
    "ai.lum"        %% "odinson-core"          % odinsonVersion  )
}
