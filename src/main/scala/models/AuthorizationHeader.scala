package models

import java.util.Base64

case class AuthorizationHeader(username: String, password: String)

object AuthorizationHeader {
  def apply(header: String): Option[AuthorizationHeader] = {
    val decodedString =
      try {
        new String(Base64.getDecoder.decode(header.substring(6)))
      } catch {
        case _: Throwable => ""
      }
    if (decodedString.length > 0) {
      val Array(username, password) = decodedString.split(":", 2)
      Some(new AuthorizationHeader(username, password))
    } else {
      Option.empty[AuthorizationHeader]
    }
  }
}
