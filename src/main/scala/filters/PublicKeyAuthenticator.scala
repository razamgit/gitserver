package filters

import java.security.PublicKey

import models.{ AuthType, Database, PublicKeyConstructor }
import org.apache.sshd.common.AttributeStore
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.session.ServerSession
import org.slf4j.LoggerFactory
import repositories.RzRepository

object PublicKeyAuthenticator {
  // put in the ServerSession here to be read by GitCommand later
  private val authTypeSessionKey = new AttributeStore.AttributeKey[AuthType]

  def putAuthType(serverSession: ServerSession, authType: AuthType): Unit =
    serverSession.setAttribute(authTypeSessionKey, authType)

  def getAuthType(serverSession: ServerSession): Option[AuthType] =
    Option(serverSession.getAttribute(authTypeSessionKey))
}

class PublicKeyAuthenticator(db: Database) extends PublickeyAuthenticator {
  private val logger = LoggerFactory.getLogger(classOf[PublicKeyAuthenticator])
  val rzRepository   = new RzRepository(db)

  override def authenticate(username: String, key: PublicKey, session: ServerSession): Boolean =
    authenticateLoginUser(username, key, session)

  private def authenticateLoginUser(userName: String, key: PublicKey, session: ServerSession): Boolean = {

    val userSshKeys =
      rzRepository.sshKeysByUserName(userName).flatMap(userKey => PublicKeyConstructor.fromString(userKey.publicKey))

    if (userSshKeys.contains(key)) {
      logger.info(s"authentication as ssh user ${userName} succeeded")
      PublicKeyAuthenticator.putAuthType(session, AuthType.UserAuthType(userName))
      true
    } else {
      logger.info(s"authentication as ssh user ${userName} failed")
      false
    }
  }
}
