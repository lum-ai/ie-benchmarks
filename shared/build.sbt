name := "shared"

libraryDependencies ++= {
  val procVersion   = "7.5.1"

  Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.clulab"    %% "processors-main"     % procVersion,
    "ai.lum" %% "common" % "0.0.8"
  )
}
