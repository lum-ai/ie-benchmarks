lazy val commonScalacOptions = Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  // "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-encoding", "utf8"
)

lazy val commonSettings = Seq(
  organization := "ai.lum",
  scalaVersion := "2.12.6",
  // we want to use -Ywarn-unused-import most of the time
  scalacOptions ++= commonScalacOptions,
  scalacOptions += "-Ywarn-unused-import",
  // -Ywarn-unused-import is annoying in the console
  scalacOptions in (Compile, console) := commonScalacOptions,
  // show test duration
  testOptions in Test += Tests.Argument("-oD"),
  // avoid dep. conflict in assembly task for webapp
  excludeDependencies += "commons-logging" % "commons-logging",
  parallelExecution in Test := false
)

lazy val shared = (project in file("shared"))
  .settings(commonSettings)

lazy val odin = (project in file("odin"))
  .settings(commonSettings)
  .aggregate(shared)
  .dependsOn(shared)

lazy val odinson = (project in file("odinson"))
  .settings(commonSettings)
  .aggregate(shared)
  .dependsOn(shared)
