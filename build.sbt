ThisBuild / organization := "com.github.ancane"
ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.5-SNAPSHOT"

val Deps = new {
  private val akkaHttpVersion = "10.1.13"
  private val akkaVersion = "2.5.32"

  private val sprayJson = "io.spray" %% "spray-json" % "1.3.6"
  private val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  private val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  private val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  private val akka = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  private val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  private val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion % Test
  private val specs2Core = "org.specs2" %% "specs2-core" % "4.10.6" % Test

  val all = Seq(
        sprayJson,
        akkaHttp,
        akkaHttpSprayJson,
        akkaHttpTestKit,
        akka % "provided",
        akkaTestkit,
        specs2Core,
        akkaStream
      )
}

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xlog-reflective-calls",
  "-Xlint"
)

lazy val root = (project in file("."))
  .settings(
    name := "haldr",
    scalaVersion := "2.13.4",
    crossScalaVersions := Seq("2.11.12", "2.12.13", "2.13.4"),
    description := "HAL builder for spray-json",
    publishTo := sonatypePublishToBundle.value,
    libraryDependencies ++= Deps.all
  )
