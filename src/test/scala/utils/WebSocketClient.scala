package utils

import com.github.andyglow.websocket._
import com.onairentertainment.Config

import scala.io.StdIn


object WebSocketClient {
  def main(args: Array[String]): Unit = {
    val host = Config.config.server.host
    val port = Config.config.server.port
    val endPoint = Config.config.server.endpoint

    val hostString: String = s"ws://$host:$port/$endPoint"
    println(s"Try to connect to $hostString")
    val cli = WebsocketClient[String](hostString) {
      case str =>
        println(s"$str")
    }
    val ws = cli.open()

    var pingId: Int = 0
    def pingRequest: String = {
      pingId = pingId + 1
      s"""
        |{
        |   "message_type": "request.ping",
        |   "id":$pingId,
        |   "timestamp": ${System.currentTimeMillis()}
        |}
        |""".stripMargin
    }

    def gameRequest(playersNumber: Int): String =
      s"""
         |{
         |   "message_type": "request.play",
         |   "players": $playersNumber
         |}
         |""".stripMargin

    val gamePattern = "(game|g) (\\d+)".r

    val usageString =
      """
        |Usage:
        |   exit -- to exit
        |   ping -- send ping to webserver
        |   game N -- send game request to the server where N is player numbers, f.e. "game 5"
        |   Any other string will be considered as raw json and will be sent to the server
        |""".stripMargin
    println(usageString)

    var command: String = ""
    while (command != "exit") {
      command = StdIn.readLine()
      val request =
        command match {
        case "ping" | "p" => pingRequest
        case gamePattern(_, playersNumber) => gameRequest(playersNumber.toInt)
        case "exit" => ""
        case request => request
      }
      if (request.nonEmpty) {
        println("Send next request to the server:")
        println(request)
        ws ! request
      }
    }

    cli.shutdownSync()
  }
}
