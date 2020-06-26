package models

import java.security.PublicKey
import java.util.Base64

import org.apache.sshd.common.util.buffer.ByteArrayBuffer
import org.eclipse.jgit.lib.Constants

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
