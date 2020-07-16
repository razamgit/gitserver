package filters

import java.security.PublicKey

import models.{ Account, AccountWKey, Database, PublicKeyConstructor }
import org.apache.sshd.common.AttributeStore
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.session.ServerSession
import org.slf4j.LoggerFactory
import repositories.RzEntitiesRepository

object RzPublickeyAuthenticator {
  // put in the ServerSession here to be read by GitCommand later
  private val authTypeSessionKey = new AttributeStore.AttributeKey[Account]

  def putAuthType(serverSession: ServerSession, account: Account): Unit =
    serverSession.setAttribute(authTypeSessionKey, account)

  def getAuthType(serverSession: ServerSession): Option[Account] =
    Option(serverSession.getAttribute(authTypeSessionKey))
}

class RzPublickeyAuthenticator(db: Database) extends PublickeyAuthenticator {
  private val logger = LoggerFactory.getLogger(classOf[RzPublickeyAuthenticator])
  val rzRepository   = new RzEntitiesRepository(db)

  override def authenticate(username: String, key: PublicKey, session: ServerSession): Boolean =
    authenticateLoginUser(username, key, session)

  private def authenticateLoginUser(userName: String, accountKey: PublicKey, session: ServerSession): Boolean = {
    val userKeys = rzRepository.sshKeysByUserName(userName)
    val authKeys = userKeys.filter(key => PublicKeyConstructor.fromString(key.publicKey).getOrElse(None) == accountKey)
    authKeys.headOption match {
      case Some(authKey) =>
        logger.info(s"Authentication as ssh user $userName succeeded")
        RzPublickeyAuthenticator.putAuthType(session, AccountWKey(userKeys.head.accountId, userName, authKey))
        true
      case _ =>
        logger.info(s"authentication as ssh user $userName failed")
        false
    }
  }
}
