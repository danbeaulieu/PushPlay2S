package models.pushplay2s

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.iteratee.Concurrent.{Channel => PlayChannel}
import collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic._

abstract class Channel(val name: String) {

  def subscribe(message: JsObject, socket_id: String, channel: PlayChannel[JsValue]): Message
  
  def unsubscribe(socket_id: String)

  def notifyAll(message: JsValue)

}

class PublicChannel(name: String) extends Channel(name) {
  
  val subscribers = scala.collection.mutable.Map[String, PlayChannel[JsValue]]()
  
  def subscribe(message: JsObject, socket_id: String, subscriberChannel: PlayChannel[JsValue]): Message = { 
    
    PubSub.subscribe(name)
    subscribers.put(socket_id, subscriberChannel)
    Message("pusher_internal:subscription_succeeded", name, null)
  }

  def unsubscribe(socket_id: String) = {

    subscribers.remove(socket_id)
    if (subscribers.isEmpty) {
      // TODO this could be a race condition...
      PubSub.unsubscribe(name)    
    }
  }

  def notifyAll(message: JsValue) = 
  
    subscribers.values.map(_.push(message))
    
}

class PrivateChannel(name: String) extends PublicChannel(name) {

  override def subscribe(message: JsObject, socket_id: String, subscriberChannel: PlayChannel[JsValue]): Message = {
    
    if (!Authenticator.isValidSignature(message, socket_id)) {
      Message("pusher:error", null, toJson(Map("error" -> "Invalid Authentication Signature")))  
    } 
    else 
      return super.subscribe(message, socket_id, subscriberChannel)
  }
}

class PresenceChannel(name: String) extends PrivateChannel(name) {
}

object Channel {
  
  val channels = TrieMap[String, Channel]()
  
  def findOrCreateChannel(channelName: String): Channel = {
    
    if (channelName.startsWith("presence-"))
      channels.putIfAbsent(channelName, new PresenceChannel(channelName))
    else if (channelName.startsWith("private-"))
      channels.putIfAbsent(channelName, new PrivateChannel(channelName))
    else
      channels.putIfAbsent(channelName, new PublicChannel(channelName))
    channels.get(channelName).get
  }

  def find(channelName: String): Option[Channel] = {
    channels.get(channelName)
  }

  def isAuthenticated(channelName: String): Boolean = {
    return channelName.startsWith("private-") || channelName.startsWith("presence-")  
  }
}
