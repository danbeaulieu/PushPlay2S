package filters

import play.api.Logger
import play.api.libs.iteratee._
import play.api.libs.json._

class MessageFilters {

  val removeChannelFilter = Enumeratee.map[JsValue](removeNullChannel)

  val removeDataFilter = Enumeratee.map[JsValue](removeNullData)

  val allFilters = removeChannelFilter.compose(removeDataFilter)

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
}