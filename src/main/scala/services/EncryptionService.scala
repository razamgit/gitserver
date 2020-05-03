package services

import java.util.Base64

import javax.servlet.http.HttpServletResponse
import org.mindrot.jbcrypt.BCrypt

/**
 * Provides HTTP (Basic) Authentication related functions.
 */
object EncryptionService {
  def decodeAuthHeader(header: String): String = {
    try {
      new String(Base64.getDecoder.decode(header.substring(6)))
    } catch {
      case _: Throwable => ""
    }
  }

  def checkHash(str: String, strHashed: String): Boolean = {
    BCrypt.checkpw(str, strHashed)
  }
}
