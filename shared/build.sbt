
name := "benchmarks-shared"

libraryDependencies ++= {

  val scoptVersion = "4.0.0-RC2"
  Seq(
    "com.github.scopt" %% "scopt" % scoptVersion
  )
}