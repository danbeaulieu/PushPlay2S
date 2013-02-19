package models

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.functional.syntax._

case class Message(event: String, channel: String, data: JsValue)

object Message {
  implicit val messageWrites = Json.writes[Message]
}
