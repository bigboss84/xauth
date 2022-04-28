package io.xauth.util

import java.io.{File, FileInputStream}
import java.security.{MessageDigest, SecureRandom}
import java.text.SimpleDateFormat
import java.util.Base64.getEncoder
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{KeyGenerator, SecretKey}
import scala.io.Source
import scala.util.Random

/**
  * Implicit helpers
  */
object Implicits {

  /**
    * Converts a string to a new string that represents
    * `MD5` hash of the input string.
    *
    * @param s String to convert.
    */
  implicit class Md5String(s: String) {
    def md5: String =
      MessageDigest.getInstance("MD5")
        .digest(s.getBytes).map(0xFF & _).map("%02x".format(_)).foldLeft("") {
        _ + _
      }
  }

  implicit class Base64String(s: String) {
    def base64: String = getEncoder.encodeToString(s.getBytes)
  }

  /**
    * Generates a random string.
    *
    * @param c A sequence of character used to make string.
    */
  implicit class RandomString(c: Seq[Char]) {
    val random: Random = new SecureRandom

    def random(length: Int): String = {
      val sb = new StringBuilder
      for (i <- 1 to length) {
        val randomNum = random.nextInt(c.length)
        sb.append(c(randomNum))
      }
      sb.toString
    }
  }

  implicit class EmailMasker(c: String) {
    val random: SecureRandom = new SecureRandom

    def mask: String = {
      val s = c.splitAt(c.indexOf('@'))
      s._1.map {
        c => if (random.nextInt(10) < 6) "*" else c
      }.mkString + '@' + s._2.substring(1)
    }
  }

  implicit class FileSource(file: File) {
    def map[T](f: Source => T): T = {
      val bs = Source.fromFile(file)
      try f(bs) finally try bs.close catch {
        case e: Exception => Unit
      }
    }
    def bytes: Array[Byte] = {
      val fis = new FileInputStream(file)
      try Stream.continually(fis.read).takeWhile(-1 !=).map(_.toByte).toArray
      finally fis.close()
    }
  }

  /**
    * Helper object to read bytes as private/public key.
    */
  implicit class KeyBytes(bytes: Array[Byte]) {
    import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
    import java.security.{KeyFactory, PrivateKey, PublicKey}

    def toPrivateKey: PrivateKey = {
      val spec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes)
      val kf: KeyFactory = KeyFactory.getInstance("RSA")
      kf.generatePrivate(spec)
    }

    def toPublicKey: PublicKey = {
      val spec: X509EncodedKeySpec = new X509EncodedKeySpec(bytes)
      val kf: KeyFactory = KeyFactory.getInstance("RSA")
      kf.generatePublic(spec)
    }

    def toSecretKey: SecretKey = new SecretKeySpec(bytes, 0, bytes.length, "RSA")
  }

  implicit class FormattedDate(date: Date) {
    import io.xauth.service.mongo.BsonHandlers.iso8601DateFormat
    private val formatter = new SimpleDateFormat(iso8601DateFormat)
    val toIso8601: String = formatter.format(date)
  }

}
