name := "YahooFinanceHistoryFetcher"

organization := "com.github.fsw0422"

version := "0.1.1"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

homepage := Some(url("http://blog.sigkill9.com"))

publishTo := Some(
  if (isSnapshot.value) {
    Opts.resolver.sonatypeSnapshots
  } else {
    Opts.resolver.sonatypeStaging
  }
)

publishMavenStyle := true

scmInfo := Some(
  ScmInfo(
    url("https://github.com/fsw0422/YahooFinanceHistoryFetcher"),
    "scm:git@github.com:fsw0422/YahooFinanceHistoryFetcher.git"
  )
)

developers := List(
  Developer(
    id = "fsw0422",
    name = "Kevin Kwon",
    email = "fsw0422@gmail.com",
    url = url("http://blog.sigkill9.com")
  )
)

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.11.11")

val akkaHttp = "10.0.10"

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.1.0",
  "com.typesafe.akka" %% "akka-http" % akkaHttp,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttp % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")
publishArtifact in Test := false
fork in Test := true

lazy val YahooFinanceHistoryFetcher = project in file(".")
