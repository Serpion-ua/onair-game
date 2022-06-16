package com.onairentertainment

import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.onairentertainment.Protocol._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike

import scala.reflect.ClassTag

class WebSocketSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest with Directives {
  implicit val jsonConfig: Configuration = JsonUtils.jsonConfig

  val gameRoute: Route =
    path( "game") {
      handleWebSocketMessages(new WsRouter().websocketFlow())
    }

  def getResponse[T <: game_response : ClassTag](wsClient: WSProbe): Assertion = {
    val response = wsClient.expectMessage().asTextMessage.getStrictText
    decode[game_response](response) match {
      case Right(results) => results mustBe a[T]
      case Left(error) => fail(s"Failed parse response due: $error")
    }
  }

  val pingRequest: String =
    """
      |{
      |   "message_type": "request.ping",
      |   "id":90,
      |   "timestamp": 1655142824279
      |}
      |""".stripMargin

  val gameRequest: String =
    """
      |{
      |   "message_type": "request.play",
      |   "players": 7
      |}
      |""".stripMargin

  def getResponse[T <: game_response : ClassTag](wsClient: WSProbe, expectedResult: T)
                                                (check: T => Boolean = (_: T) => true): Assertion = {
    val response = wsClient.expectMessage().asTextMessage.getStrictText
    decode[game_response](response) match {
      case Right(`expectedResult`) =>
        check(expectedResult) mustBe true
      case Right(nonExpectedResult) =>
        fail(s"Response $nonExpectedResult in not match expected response $expectedResult")
      case Left(error) =>
        fail(s"Failed parse response due: $error")
    }
  }

  "WebSocket" must {
    "correctly send pong on ping request" in {
      val wsClient = WSProbe()
      val timeBeforePing = System.currentTimeMillis()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(pingRequest)
          val response = wsClient.expectMessage().asTextMessage.getStrictText
          val timeAfterPing = System.currentTimeMillis()
          decode[game_response](response) match {
            case Right(`response.pong`(90, 1655142824279L, timestamp @ _)) =>
              timestamp mustBe >= (timeBeforePing)
              timestamp mustBe <= (timeAfterPing)
            case Right(value) => fail(
              s"Unexpected response: expected `response.pong`(90, 1655142824279L, _); but actual is: $value")
            case Left(error) => fail(s"Failed parse response due: $error")
          }

          val timeBeforeSecondPing = System.currentTimeMillis()
          wsClient.sendMessage(pingRequest)
          val secondPingResponse = wsClient.expectMessage().asTextMessage.getStrictText
          val timeAfterSecondPing = System.currentTimeMillis()
          decode[game_response](secondPingResponse) match {
            case Right(`response.pong`(90, 1655142824279L, timestamp @ _)) =>
              timestamp mustBe >= (timeBeforeSecondPing)
              timestamp mustBe <= (timeAfterSecondPing)
            case Right(value) => fail(
              s"Unexpected response: expected `response.pong`(90, 1655142824279L, _); but actual is: $value")
            case Left(error) => fail(s"Failed parse response due: $error")
          }

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response on missed id field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.ping",
              |   "timestamp": 1655142824279
              |}
              |""".stripMargin
          wsClient.sendMessage(request)

          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("id"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response on missed message_type field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "id":90,
              |   "timestamp": 1655142824279
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("message_type"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response on missed id and timestamp fields" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |  "message_type": "request.ping"
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("id"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response on incorrect message_type field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "id":90,
              |   "message_type": "request.Ping",
              |   "timestamp": 1655142824279
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.incorrectMessageTypeMessage
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response on missed timestamp field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.ping",
              |   "id":90
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("timestamp"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if ping request contains additional fields" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.ping",
              |   "id":90,
              |   "timestamp": 1655142824279,
              |   "additional_field": true
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = "Unexpected field: [additional_field]; valid fields: id, timestamp, message_type"
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly return game result on request.play" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(gameRequest)
          getResponse[`response.results`](wsClient)

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly return not empty game result on deterministic game logic" in {
      val deterministicGameRoute: Route =
        path( "game") {
          handleWebSocketMessages(new WsRouter().websocketFlow(new GameLogic(42, 999999)))
        }

      val expectedGameResults =
        Seq(GameResultForPlayer(1,5,969970,913), GameResultForPlayer(2,4,948884,849))


      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> deterministicGameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(gameRequest)
          getResponse(wsClient, `response.results`(expectedGameResults))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }

    }

    "correctly return empty game result on deterministic game logic" in {
      val deterministicGameRoute: Route =
        path( "game") {
          handleWebSocketMessages(new WsRouter().websocketFlow(new GameLogic(43, 999999)))
        }

      val expectedGameResults = Seq.empty[GameResultForPlayer]

      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> deterministicGameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.play",
              |   "players": 3
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          getResponse(wsClient, `response.results`(expectedGameResults))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if game request contains additional fields" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.play",
              |   "players": 3,
              |   "additional_field": true
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = "Unexpected field: [additional_field]; valid fields: players, message_type"
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if game request have zero players fields" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.play",
              |   "players": 0
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = GameLogic.playersCountErrorMessage
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if game request have no players field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "message_type": "request.play"
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("players"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if game request have message_type field" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |   "players": 0
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("message_type"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly send response if request is empty" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          val request =
            """
              |{
              |}
              |""".stripMargin
          wsClient.sendMessage(request)
          val expectedErrorMessage = JsonUtils.missedRequiredField(Seq("message_type"))
          getResponse(wsClient, `response.failed`(expectedErrorMessage))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "game works after sending malformed json" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(gameRequest)
          getResponse[`response.results`](wsClient)


          val malformedRequest =
            """
              |{
              |   "message_type": "request.play"
              |   "players": 5
              |}
              |""".stripMargin
          wsClient.sendMessage(malformedRequest)
          getResponse[`response.failed`](wsClient)


          wsClient.sendMessage(gameRequest)
          getResponse[`response.results`](wsClient)


          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "game return different results after the same request" in {
      val deterministicGameRoute: Route =
        path( "game") {
          handleWebSocketMessages(new WsRouter().websocketFlow(new GameLogic(43, 999999)))
        }

      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> deterministicGameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(gameRequest)
          val firstResponse = wsClient.expectMessage().asTextMessage.getStrictText
          val firstResult =
            decode[game_response](firstResponse) match {
            case Right(`response.results`(results)) => results
            case Right(response) => fail(s"Unexpected response $response")
            case Left(error) => fail(s"Failed parse response due: $error")
          }

          wsClient.sendMessage(gameRequest)
          val secondResponse = wsClient.expectMessage().asTextMessage.getStrictText
          val secondResult =
            decode[game_response](secondResponse) match {
            case Right(`response.results`(results)) => results
            case Right(response) => fail(s"Unexpected response $response")
            case Left(error) => fail(s"Failed parse response due: $error")
          }

          firstResult != secondResult mustBe true

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly doesn't process binary data" in {
      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> gameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(gameRequest)
          getResponse[`response.results`](wsClient)

          wsClient.sendMessage(BinaryMessage(ByteString("binary_data")))
          wsClient.expectNoMessage(100.millis)

          wsClient.sendMessage(BinaryMessage.Streamed(Source.single(ByteString("binary_data"))))
          wsClient.expectNoMessage(100.millis)

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "correctly process text streaming data" in {
      val deterministicGameRoute: Route =
        path( "game") {
          handleWebSocketMessages(new WsRouter().websocketFlow(new GameLogic(42, 999999)))
        }

      val expectedGameResults =
        Seq(GameResultForPlayer(1,5,969970,913), GameResultForPlayer(2,4,948884,849))

      val wsClient = WSProbe()
      WS("/game", wsClient.flow) ~> deterministicGameRoute ~>
        check {
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage(TextMessage.Streamed(Source.single(gameRequest)))
          getResponse(wsClient, `response.results`(expectedGameResults))()

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

  }

}
