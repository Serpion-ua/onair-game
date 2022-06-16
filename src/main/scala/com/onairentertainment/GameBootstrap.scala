package com.onairentertainment

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object GameBootstrap extends App {
  implicit val system: ActorSystem = ActorSystem("app")

  val host = Config.config.server.host
  val port = Config.config.server.port
  val endPoint = Config.config.server.endpoint
  val gameRoute: Route =
    path( endPoint) {
        handleWebSocketMessages(new WsRouter().websocketFlow())
    }

  val bindingFuture =
    Http().newServerAt(host, port).bindFlow(gameRoute)
    .map { f =>
      println(s"Server is running at http://$host:$port/$endPoint press any key to terminate")
      f
    }

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
