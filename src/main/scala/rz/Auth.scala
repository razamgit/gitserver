package rz

import java.util.Base64
import org.mindrot.jbcrypt.BCrypt

import javax.servlet.http.HttpServletResponse


/**
 * Provides HTTP (Basic) Authentication related functions.
 */
object Auth {
  def requireAuth(response: HttpServletResponse): Unit = {
    response.setHeader("WWW-Authenticate", "BASIC realm=\"razam\"")
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
  }

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
