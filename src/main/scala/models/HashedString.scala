package models

import org.mindrot.jbcrypt.BCrypt

case class HashedString(hash: String) {
  override def toString: String = hash

  def check(unencrypted: String): Boolean = BCrypt.checkpw(unencrypted, hash)
}

object HashedString {
  def fromString(unencryptedString: String): HashedString =
    HashedString(BCrypt.hashpw(unencryptedString, BCrypt.gensalt()))
}
