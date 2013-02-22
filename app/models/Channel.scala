package models.pushplay2s

import play.api._
import play.api.Play.current
import com.typesafe.plugin.RedisPlugin
import play.api.Play.current
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.iteratee.Concurrent.{Channel => PlayChannel}
import collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic._

abstract class Channel(val name: String) {
  
  lazy val pool = Play.application.plugin(classOf[RedisPlugin]).get.sedisPool

  PubSub.subscribe(name)

  val subscribers = scala.collection.mutable.Map[String, PlayChannel[JsValue]]()

  def subscribe(message: JsObject, socket_id: String, channel: PlayChannel[JsValue]): Message
  
  def unsubscribe(socket_id: String)

  def notifyAll(message: JsValue)

}

class PublicChannel(name: String) extends Channel(name) {
  
  def subscribe(message: JsObject, socket_id: String, subscriberChannel: PlayChannel[JsValue]): Message = { 
    
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
  import scala.collection.JavaConverters._
  import scala.collection.mutable.Buffer

  lazy val members: Buffer[JsValue] = { 
    pool.withJedisClient{ client =>
      asScalaBufferConverter(client.lrange(name, 0, -1)).asScala.map(Json.parse(_)).toBuffer
    }
  }

  override def subscribe(message: JsObject, socket_id: String, subscriberChannel: PlayChannel[JsValue]): Message = {
    
    if (!Authenticator.isValidSignature(message, socket_id)) {
      Message("pusher:error", null, toJson(Map("error" -> "Invalid Authentication Signature")))  
    } 
    else {
      // send new subscriber notification to redis
      PubSub.publish("pushplay2s:presence_updates", Json.toJson(
        Message("add_member", name, Json.toJson(Map("channel_data" -> (message \ "data" \ "channel_data"))))
      ))
      // update redis roster
      pool.withJedisClient{ client =>
        client.rpush(name, (message \ "data" \ "channel_data").as[String])
      }
      // return message with channel_data
      subscribers.put(socket_id, subscriberChannel)
      return Message("pusher_internal:subscription_succeeded", name, toJson(
        Map("presence" -> toJson(
          Map("count" -> toJson(members.size),
              "ids" -> toJson(memberIds),
              "hash" -> toJson(memberHash))))
      ))
    }
  }
  
  def memberIds =
    members.map{ member => (member \ "user_id").as[String] }

  def memberHash =
    members.map{ member => ((member \ "user_id").as[String] -> (member \ "user_info"))}.toMap

  def notifyNewMember(channel_data: JsValue) = {
    if (!members.contains(channel_data)) {
      notifyAll(Json.toJson(Message(name, "pusher_internal:member_added", channel_data)))
    }
    members.append(channel_data)
  }
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
