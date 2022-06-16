package com.onairentertainment
import akka.actor.{Actor, ActorRef}
import com.onairentertainment.Protocol._

class GameActor(gameLogic: GameLogic) extends Actor {
  var client: ActorRef = _

  override def receive: Receive = {
    case actorRef: ActorRef => client = actorRef
    case `request.play`(players) => gameLogic.startGame(players) match {
      case Right(results) => client ! `response.results`(results)
      case Left(errorMessage) => client ! `response.failed`(errorMessage)
    }
    case `request.ping`(id, timestamp) => client ! `response.pong`(id, timestamp, System.currentTimeMillis())
    case `request.incorrect`(errorMessage) => client ! `response.failed`(errorMessage)
  }
}