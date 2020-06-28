package models

import java.security.PublicKey
import java.util.Base64

import org.apache.sshd.common.util.buffer.ByteArrayBuffer
import org.eclipse.jgit.lib.Constants
import org.mindrot.jbcrypt.BCrypt

sealed trait AuthType

object AuthType {
  case class UserAuthType(userName: String)      extends AuthType
  case class DeployKeyType(publicKey: PublicKey) extends AuthType

  /**
   * Retrieves username if authType is UserAuthType, otherwise None.
   */
  def userName(authType: AuthType): Option[String] =
    authType match {
      case UserAuthType(userName) => Some(userName)
      case _                      => None
    }
}

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

case class HashedString(hash: String) {
  override def toString: String = hash

  def check(unencrypted: String): Boolean = BCrypt.checkpw(unencrypted, hash)
}

object HashedString {
  def fromString(unencryptedString: String): HashedString =
    HashedString(BCrypt.hashpw(unencryptedString, BCrypt.gensalt()))
}

case class SshKey(accountId: Long, publicKey: String)

object PublicKeyConstructor {
  def fromString(key: String): Option[PublicKey] = {
    // TODO RFC 4716 Public Key is not supported...
    val parts = key.split(" ")
    if (parts.size < 2) {
      None
    } else {
      try {
        val encodedKey = parts(1)
        val decode     = Base64.getDecoder.decode(Constants.encodeASCII(encodedKey))
        Some(new ByteArrayBuffer(decode).getRawPublicKey)
      } catch {
        case e: Throwable =>
          None
      }
    }
  }
}
