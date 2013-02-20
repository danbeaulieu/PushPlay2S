import play.api._

import redis.clients.jedis.Jedis
import com.typesafe.plugin.RedisPlugin
import play.api.Play.current
import play.api.libs.concurrent.Akka
import models.pushplay2s.PubSub

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    
    Logger.debug("subscribing to redis")
    Akka.future { 
      val j = new RedisPlugin(app).jedisPool.getResource
      j.subscribe(PubSub, "*")
    }
  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
}
