package repositories

import java.security.KeyStore.TrustedCertificateEntry

import anorm.SqlParser.get
import anorm._
import models.{ Database, HashedString, SshKey }

case class Repository(id: Long)

class RzRepository(db: Database) {

  /**
   * Parse a Hashed Password from a ResultSet
   */
  val passwordParser: RowParser[HashedString] = {
    get[String]("account.password").map(password => HashedString(password))
  }

  /**
   * Parse a Hashed Password from a ResultSet
   */
  val sshKeyParser: RowParser[SshKey] = {
    (get[Long]("ssh_keys.account_id") ~ get[String]("ssh_keys.public_key")).map {
      case accountId ~ publicKey => SshKey(accountId, publicKey)
    }
  }

  /**
   * Parse a Repository from a ResultSet
   */
  private val simpleRepository = {
    get[Long]("repository.id").map(id => Repository(id))
  }

  def getRepository(repositoryOwner: String, repositoryName: String): Option[Repository] =
    db.withConnection { implicit connection =>
      SQL("""select repository.id from repository
             join account on account.id = repository.owner_id
             where account.username = {repositoryOwner}
             and repository."name" = {repositoryName}
             """)
        .on("repositoryOwner" -> repositoryOwner, "repositoryName" -> repositoryName)
        .as(simpleRepository.singleOpt)
    }

  def isUserWithPasswordExists(username: String, password: String): Boolean =
    db.withConnection { implicit connection =>
      SQL("select password from account where account.username = {username}")
        .on("username" -> username)
        .as(passwordParser.singleOpt) match {
        case Some(passwordHashed) => passwordHashed.check(password)
        case None                 => false
      }
    }

  def sshKeysByUserName(username: String): List[SshKey] =
    db.withConnection { implicit connection =>
      SQL("""
          select ssh_keys.account_id, ssh_keys.public_key from ssh_keys
          join account on account.id = ssh_keys.account_id
          where account.username = {username}
          """)
        .on("username" -> username)
        .as(sshKeyParser.*)
    }

  def isKeyExist(username: String, sshKey: SshKey): Boolean =
    db.withConnection { implicit connection =>
      SQL("""
          select ssh_keys.account_id, ssh_keys.public_keys
          join account on account.id = ssh_keys.account_id
          where account.username = {username}
          and ssh_keys.public_key = {publicKey}
          """)
        .on("username" -> username, "publicKey" -> sshKey.publicKey)
        .as(sshKeyParser.singleOpt) match {
        case Some(_key) => true
        case _          => false
      }
    }
}
