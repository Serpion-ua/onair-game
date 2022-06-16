import Dependencies._

name := "onair-game"
organization := "com.onairentertainment"
version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++=
  Akka.typed ++ Circe.deps ++ Seq(Common.config, Specs.webSocketClient % Test) ++
    Seq(Akka.akkaTestkit, Akka.streamTestkit, Akka.akkaHttpTestkit, Specs.scalaTest % Test)

mainClass in (Compile, run) := Some("com.onairentertainment.GameBootstrap")

TaskKey[Unit]("wsServer") := (runMain in Compile).toTask(" com.onairentertainment.GameBootstrap").value
TaskKey[Unit]("wsClient") := (runMain in Test).toTask(" utils.WebSocketClient").value
