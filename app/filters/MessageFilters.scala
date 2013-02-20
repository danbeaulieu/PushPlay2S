package filters.pushplay2s

import play.api.Logger
import play.api.libs.iteratee._
import play.api.libs.json._

class MessageFilters {

  val removeChannelee = Enumeratee.map[JsValue](removeNullChannel)

  val removeDataee = Enumeratee.map[JsValue](removeNullData)

  val removeSocketIdee = Enumeratee.map[JsValue](removeSocketId)

  val allFilters = removeChannelee.compose(removeDataee).compose(removeSocketIdee)

  def removeNullChannel(in: JsValue): JsValue = {
    in match {
      case fix: JsObject if (fix \ "channel") == JsString(null) => {
        fix - "channel"
      }
      case _ => in
    }  
  }
  
  def removeNullData(in: JsValue): JsValue = { 
    in match {
      case fix: JsObject if (fix \ "data") == null => {
        fix - "data"
      }
      case _ => in
    }
  }

  def removeSocketId(in: JsValue): JsValue = {
    in.as[JsObject] - "socket_id"
  }
}
