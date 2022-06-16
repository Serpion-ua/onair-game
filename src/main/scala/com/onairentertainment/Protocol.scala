package com.onairentertainment

/**
 * Names of case class itself and it's fields name are part of protocol.
 * Those name are used for coding and encoding request/response JSONs
 */
object Protocol {
  /**
   * Value of that key in Json is a name of appropriate case class, f.e. for next Json
   * {
   *   message_type = "request.play"
   *   ...
   * }
   * will be decode to case class request.play
   */
  val messageTypeString: String = "message_type"

  sealed trait game_request
  final case class `request.play`(players: Int) extends game_request
  final case class `request.ping`(id: Int, timestamp: Long) extends game_request
  final case class `request.incorrect`(error_message: String) extends game_request


  sealed trait game_response
  final case class `response.results`(results: Seq[GameResultForPlayer]) extends game_response
  final case class `response.pong`(request_id: Int, request_at: Long, timestamp: Long) extends game_response
  final case class `response.failed`(error_message: String) extends game_response
}
