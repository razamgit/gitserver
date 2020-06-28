package repositories

import java.security.KeyStore.TrustedCertificateEntry

import anorm.SqlParser.get
import anorm._
import models.{ AccessLevel, AccountWKey, AccountWPassword, Database, HashedString, SshKey }

case class Repository(id: Long)

class RzRepository(db: Database) {

  /**
   * Parse an Account with Hashed Password from a ResultSet
   */
  val passwordParser: RowParser[AccountWPassword] = {
    (get[Long]("account.id") ~
      get[String]("account.password") ~
      get[Option[Int]]("collaborator.role")).map {
      case accountId ~ password ~ role => AccountWPassword(accountId, HashedString(password), role)
    }
  }

  /**
   * Parse an Account With Public Key from a ResultSet
   */
  val sshKeyParser: RowParser[AccountWKey] = {
    (get[Long]("ssh_key.account_id")
      ~ get[String]("ssh_key.public_key")
      ~ get[Option[Int]]("collaborator.role")).map {
      case accountId ~ publicKey ~ role => AccountWKey(accountId, SshKey(accountId, publicKey), role)
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

  def userWithPassword(username: String, password: String): Option[AccountWPassword] =
    db.withConnection { implicit connection =>
      SQL("""
          select password from account
          left join collaborator on collaborator.user_id = account.id
          where account.username = {username}
          """)
        .on("username" -> username)
        .as(passwordParser.singleOpt)
    }

  def sshKeysByUserName(username: String): List[AccountWKey] =
    db.withConnection { implicit connection =>
      SQL("""
          select ssh_key.account_id, ssh_key.public_key, collaborator.role from ssh_key
          join account on account.id = ssh_key.account.id
          join collaborator on collaborator.user_id = account_id
          where account.username = {username}
          """)
        .on("username" -> username)
        .as(sshKeyParser.*)
    }
}
