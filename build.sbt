name := "sangria-jackson"
organization := "org.sangria-graphql"
mimaPreviousArtifacts := Set(
  "org.sangria-graphql" %% "sangria-jackson" % "1.0.0"
)

description := "Sangria jackson marshalling"
homepage := Some(url("http://sangria-graphql.org"))
licenses := Seq(
  "Apache License, ASL Version 2.0" -> url(
    "http://www.apache.org/licenses/LICENSE-2.0"
  )
)

scalaVersion := "2.13.4"
crossScalaVersions := Seq("2.11.12", "2.12.10", scalaVersion.value)

scalacOptions ++= Seq("-deprecation", "-feature")

scalacOptions ++= {
  if (scalaVersion.value startsWith "2.13")
    Seq("-Wunused")
  else if (scalaVersion.value startsWith "2.11")
    Seq("-target:jvm-1.7")
  else
    Seq("-target:jvm-1.8")
}
javacOptions ++= {
  if (scalaVersion.value startsWith "2.11")
    Seq("-source", "7", "-target", "7")
  else
    Seq("-source", "8", "-target", "8")
}

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria-marshalling-api" % "1.0.4",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.0",
  "org.sangria-graphql" %% "sangria-marshalling-testkit" % "1.0.3" % Test,
  "org.scalatest" %% "scalatest" % "3.1.4" % Test
)

// Publishing
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
publishTo := Some(
  if (version.value.trim.endsWith("SNAPSHOT"))
    "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
)

// Site and docs

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)
git.remoteRepo := "git@github.com:org.sangria-graphql/sangria-jackson.git"

// nice *magenta* prompt!

shellPrompt in ThisBuild := { state =>
  scala.Console.MAGENTA + Project
    .extract(state)
    .currentRef
    .project + "> " + scala.Console.RESET
}

inThisBuild(
  List(
    scalaVersion := "2.13.4",
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

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

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
    browseUrl = url("https://github.com/sangria-graphql/sangria-jackson.git"),
    connection = "scm:git:git@github.com:sangria-graphql/sangria-jackson.git"
  )
)
