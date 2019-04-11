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
  // use git commit as version
  version      := s"${git.gitHeadCommit.value.get.take(7)}",
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

lazy val buildInfoSettings = Seq(
  buildInfoPackage := "ai.lum.benchmarks",
  buildInfoOptions += BuildInfoOption.BuildTime,
  buildInfoKeys := Seq[BuildInfoKey](
    name, version, scalaVersion, sbtVersion, libraryDependencies, scalacOptions,
    "gitCurrentBranch" -> { git.gitCurrentBranch.value },
    "gitHeadCommit" -> { git.gitHeadCommit.value.getOrElse("") },
    "gitHeadCommitDate" -> { git.gitHeadCommitDate.value.getOrElse("") },
    "gitUncommittedChanges" -> { git.gitUncommittedChanges.value }
  )
)

lazy val shared = (project in file("shared"))
  .settings(commonSettings)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoSettings)

lazy val odin = (project in file("odin"))
  .settings(commonSettings)
  .aggregate(shared)
  .dependsOn(shared)

lazy val odinson = (project in file("odinson"))
  .settings(commonSettings)
  .aggregate(shared)
  .dependsOn(shared)
