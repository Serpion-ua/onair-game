package com.onairentertainment

import com.onairentertainment.Protocol._
import io.circe.CursorOp.DownField
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{CursorOp, DecodingFailure}

object JsonUtils {
  implicit val jsonConfig: Configuration =
    Configuration.default
      .withDiscriminator(Protocol.messageTypeString)
      .withStrictDecoding

  /**
   * Convert game_response to formatted Json
   * @param response game_response to be encoded
   * @return Json string
   */
  def encodeJson(response: game_response):String = response.asJson.spaces2SortKeys

  val incorrectMessageTypeMessage: String  =
    """Incorrect "message_type" value in request: correct values is request.play or request.ping"""

  def missedRequiredField(requiredFields: Seq[String]): String =
    s"Required field(s): \"${requiredFields.mkString(",")}\" are missed"

  /**
   * Try to convert json to appropriate game_request, if such conversion is failed
   * then try to make Json decoding error more readable for the end user
   * @param rawJson json to decode
   * @return appropriate case class
   */
  def decodeJson(rawJson: String): Either[io.circe.Error, game_request] = {
    decode[game_request](rawJson)
      .left
      .map(errorMessagePrettification)
  }

  /**
   * Try to make Json decoding error more readable for the end user
   * @param error initial decoding error
   * @return prettified error
   */
  //@TODO check other possible errors
  def errorMessagePrettification(error: io.circe.Error): io.circe.Error = {
    error match {
      case DecodingFailure("CNil", ops) =>
        DecodingFailure(incorrectMessageTypeMessage, ops)
      case DecodingFailure("Attempt to decode value on failed cursor", ops: List[CursorOp]) =>
        val fields =
          ops.map{
            case df: DownField => df.k
            case op => op.toString
          }
        DecodingFailure(missedRequiredField(fields), List())
      case error => error
    }
  }
}
