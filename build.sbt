name := "sangria-jackson"
organization := "org.sangria-graphql"
mimaPreviousArtifacts := Set(
  "org.sangria-graphql" %% "sangria-jackson" % "0.0.1"
)

description := "Sangria jackson marshalling"
homepage := Some(url("https://sangria-graphql.github.io/"))
licenses := Seq(
  "Apache License, ASL Version 2.0" -> url(
    "http://www.apache.org/licenses/LICENSE-2.0"
  )
)

ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.8")
ThisBuild / scalaVersion := crossScalaVersions.value.last
ThisBuild / githubWorkflowPublishTargetBranches := List()
ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Sbt(
  List("scalafmtCheckAll"),
  name = Some("Check formatting"))

scalacOptions ++= Seq("-deprecation", "-feature")
scalacOptions ++= Seq("-target:jvm-1.8")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.13"))
    Seq("-Wunused")
  else
    List.empty[String]
}

javacOptions ++= Seq("-source", "8", "-target", "8")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria-marshalling-api" % "1.1.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4",
  "org.sangria-graphql" %% "sangria-marshalling-testkit" % "1.0.4" % Test,
  "org.scalatest" %% "scalatest" % "3.2.13" % Test
)

// Release
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

// nice *magenta* prompt!
ThisBuild / shellPrompt := { state =>
  scala.Console.MAGENTA + Project
    .extract(state)
    .currentRef
    .project + "> " + scala.Console.RESET
}

inThisBuild(
  List(
    scalaVersion := "2.13.8",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

commands += Command.command("format") { state =>
  "compile:scalafixAll" ::
    "test:scalafixAll" ::
    "compile:scalafmt" ::
    "test:scalafmt" ::
    state
}

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

// Additional meta-info
startYear := Some(2020)
organizationHomepage := Some(url("https://github.com/sangria-graphql"))
developers := Developer(
  "nickhudkins",
  "Nick Hudkins",
  "",
  url("https://github.com/nickhudkins")
) :: Nil
scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/sangria-graphql/sangria-jackson"),
    connection = "scm:git:git@github.com:sangria-graphql/sangria-jackson.git"
  )
)
