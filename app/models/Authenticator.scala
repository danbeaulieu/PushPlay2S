package models.pushplay2s

import play.api.Play
import play.api.libs.json._
import play.api.libs.json.Json._

import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Authenticator {

  def computeSignature(channel: String, socket_id: String, cd: Option[String]): String = {
		
    val string_to_sign: String = Seq(Some(channel), Some(socket_id), cd).flatten.mkString(":")
		
		return sha256(string_to_sign, Play.current.configuration.getString("pusher.secret_key").get)
	}

  def sha256(string: String, secret: String): String = {
		
    try {
      val signingKey: SecretKeySpec  = new SecretKeySpec( secret.getBytes(), "HmacSHA256")

      val mac: Mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)

      val digest: Array[Byte] = mac.doFinal(string.getBytes())

      val bigInteger = new BigInteger(1, digest)
      return String.format("%0" + (digest.length << 1) + "x", bigInteger)

    } catch {
      case nsae: NoSuchAlgorithmException => 
        throw new RuntimeException("No HMac SHA256 algorithm")
      case e: InvalidKeyException => 
        throw new RuntimeException("Invalid key exception while converting to HMac SHA256")
    }
  }

  def isValidSignature(msg: JsObject, socket_id: String): Boolean = {
		(msg \ "data" \ "auth").asOpt[String].map{ auth => {
      val channel = (msg \ "data" \ "channel").as[String]
      val channel_data = (msg \ "data" \ "channel_data").asOpt[String]
      return computeSignature(channel, socket_id, channel_data).equals(auth.split(":")(1))
      }
    } getOrElse {
      return false
    }
	}
}
