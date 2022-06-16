package com.onairentertainment

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props, Status}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.onairentertainment.Protocol._

import scala.concurrent.Future


class WsRouter()(implicit system: ActorSystem, mat: Materializer) {
  private val defaultGameLogic =
    new GameLogic(System.nanoTime() ^ System.currentTimeMillis(), Config.config.gameLogic.maxNumberValue)

  def websocketFlow(gameLogic: GameLogic = defaultGameLogic): Flow[Message, Message, Any] = {

    val gameActor: ActorRef =
      system.actorOf(Props(new GameActor(gameLogic)))

    val sink =
      Flow[Message]
        .collect {
          case TextMessage.Strict(rawJson) => Future.successful(rawJson)
          case TextMessage.Streamed(stream) => stream.runFold("")(_ + _)
        }
        .mapAsync(1)(identity)
        .map(JsonUtils.decodeJson)
        .map {
          case Right(correctGameRequest) => correctGameRequest
          case Left(error) => `request.incorrect`(error.getMessage)
        }
        .to(Sink.actorRef[game_request](
          gameActor,
          PoisonPill,
          t => Status.Failure(t)))

    val sourceActor: Source[game_response, ActorRef] =
      Source.actorRef[game_response](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = 16,
        overflowStrategy = OverflowStrategy.fail)

    val source: Source[Message, ActorRef] =
      sourceActor
        .map(msg => TextMessage(JsonUtils.encodeJson(msg)))
        .mapMaterializedValue { destRef =>
          gameActor ! destRef
          destRef
        }

    Flow.fromSinkAndSourceCoupled(sink, source)
  }
}
