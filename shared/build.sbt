name := "benchmarks-shared"

libraryDependencies ++= {
  val procVersion   = "7.5.1"
  val scoptVersion = "4.0.0-RC2"

  Seq(
    "com.github.scopt" %% "scopt" % scoptVersion,
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.clulab"    %% "processors-main"     % procVersion,
    "ai.lum"        %% "common" % "0.0.8"

  )
}
