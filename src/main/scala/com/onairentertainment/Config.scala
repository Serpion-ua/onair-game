package com.onairentertainment
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Config {
  case class Application(server: Server, gameLogic: GameLogic)

  case class Server(host: String, port: Int, endpoint: String)
  case class GameLogic(maxNumberValue: Int)


  val config: Application =
    ConfigSource.fromConfig(ConfigFactory.load()).loadOrThrow[Application]
}
