package controllers.pushplay2s

import filters.pushplay2s._
import java.util.UUID
import models.pushplay2s._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import Concurrent._
import play.api.libs.json._
import play.api.libs.json.Json._

object Application extends Controller {
  
  val mFilters = new MessageFilters()

  val messageFixer = mFilters.allFilters

  def app(apiKey: String) = WebSocket.using[JsValue] { req =>
    
    val socket_id = UUID.randomUUID().toString

    val (out, ws_channel) = Concurrent.broadcast[JsValue]
    
    var subscriptions = Set[String]()
    
    val in = Iteratee.foreach[JsValue] ( _ match {
      case sub: JsObject if (sub \ "event") == JsString("pusher:subscribe") => {
        (sub \ "data" \ "channel").asOpt[String].map{ channelName => {
                    
          val channel = Channel.findOrCreateChannel(channelName)
          ws_channel.push(Json.toJson(
            channel.subscribe(sub, socket_id, ws_channel)
          ))
          subscriptions = subscriptions + channelName
          }  
        } getOrElse {
          // TODO this shouldn't ever happen
          Logger.debug("Missing channel in subscribe message")        
        }    
      }
      case unsub: JsObject if (unsub \ "event") == JsString("pusher:unsubscribe") => {
        (unsub \ "data" \ "channel").asOpt[String].map{ channelName => 
          val channel = Channel.findOrCreateChannel(channelName)
          channel.unsubscribe(socket_id)
          subscriptions = subscriptions - channelName
        } getOrElse {
          // TODO this shouldn't ever happen
          Logger.debug("Missing channel in unsubscribe message")        
        }    
      }
      case client: JsObject if (client \ "event").as[String].startsWith("client-") => {
        (client \ "channel").asOpt[String].map{ channelName => 
          if (Channel.isAuthenticated(channelName)) {
            PubSub.publish(channelName, client ++ JsObject(Seq(("socket_id", JsString(socket_id)))))
          } 
        } getOrElse {
          // TODO this shouldn't ever happen
          Logger.debug("Missing channel in unsubscribe message")        
        }    
      }
      case ping: JsObject if (ping \ "event") == JsString("pusher:ping") => {
        ws_channel.push(Json.toJson(
          Message("pusher:pong", null, null)
        ))           
      }
      case _ => { 
        Logger.info("recieved unrecoginized message")
      }
    }) mapDone {_ =>  
      Logger.debug("closing connection, reaping subs " + subscriptions.size)
      subscriptions.foreach(name => Channel.find(name) match {
        case Some(c) => {
          Logger.debug("unsubbing from channel " + name)
          c.unsubscribe(socket_id)
        }
        case None => Logger.info("Tried to unsubscribe to nonexistant channel " + name)    
      })  
    }  

    val established: Enumerator[JsValue] = {
      Enumerator(
        Json.toJson(
          Message("pusher:connection_established", null, toJson(Map("socket_id" -> toJson(socket_id))))
        )
      )
    }

    val socketIdFilter = Enumeratee.filter[JsValue](msg => (msg \ "socket_id") != JsString(socket_id))

    (in, established.through(messageFixer) >>> out.through(socketIdFilter.compose(messageFixer)))
  }
  
}
