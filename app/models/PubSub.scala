package models.pushplay2s

import com.typesafe.plugin.RedisPlugin
import play.api.Play.current
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import play.api._
import play.api.libs.json._
import play.api.libs.json.Json._

object PubSub extends JedisPubSub {


  def publish(channel: String, msg: JsObject) {
    	
    new RedisPlugin(Play.application).sedisPool.withJedisClient{ client => 
      
      val num = client.publish(channel, msg.toString)
      Logger.debug("Published message recieved by " + num + " subscribers")
    }
  }

  override def onMessage(channel: String, message: String) = {
		
    System.out.println("got message " + message)
		val m = Json.parse(message)
    m match {
      case note: JsObject if (note \ "event") == JsString("pushplay:cluster_notifications") => {
      }
      case msg: JsObject => {
        Channel.find(channel) match {
          case Some(c) => c.notifyAll(m)  
          case None => Logger.debug("Could not find channel by name=" + channel)       
        }
      }
    }
		//if (channel.equals("pushplay:cluster_notifications")) {
			
		//} else {
		//	m.put("channel", channel);
		//	ChannelService.notifyAll(channel, m);
		//}
	}

	override def onPMessage(arg0: String, arg1: String, arg2: String) = {
		// TODO Auto-generated method stub

	}

	override def onPSubscribe(arg0: String, arg1: Int) = {
		// TODO Auto-generated method stub

	}

	override def onPUnsubscribe(arg0: String, arg1: Int) = {
		// TODO Auto-generated method stub

	}

	override def onSubscribe(arg0: String, arg1: Int) = {
		// TODO Auto-generated method stub

	}

	override def onUnsubscribe(arg0: String, arg1: Int) = {
		// TODO Auto-generated method stub

	}
}
