package repositories

import anorm.SqlParser.get
import anorm._
import models._

class RzEntitiesRepository(db: Database) {

  /**
   * Parse an Account with Hashed Password from a ResultSet
   */
  val passwordParser: RowParser[AccountWPassword] = {
    (get[Long]("account.id") ~ get[String]("account.username") ~ get[String]("account.password")).map {
      case accountId ~ username ~ password => AccountWPassword(accountId, username, HashedString(password))
    }
  }

  /**
   * Parse an Account With Public Key from a ResultSet
   */
  val sshKeyParser: RowParser[SshKey] = {
    (get[Long]("ssh_key.account_id") ~ get[String]("ssh_key.public_key")).map {
      case accountId ~ publicKey => SshKey(accountId, publicKey)
    }
  }

  def repositoryId(repositoryOwner: String, repositoryName: String): Option[Int] =
    db.withConnection { implicit connection =>
      SQL("""select repository.id from repository
             join account on account.id = repository.owner_id
             where account.username = {repositoryOwner}
             and repository."name" = {repositoryName}
             """)
        .on("repositoryOwner" -> repositoryOwner, "repositoryName" -> repositoryName)
        .as(SqlParser.int("repository.id").singleOpt)
    }

  def userWithPassword(username: String, password: String): Option[AccountWPassword] =
    db.withConnection { implicit connection =>
      SQL("""
          select id, username, password from account
          where account.username = {username}
          """)
        .on("username" -> username)
        .as(passwordParser.singleOpt) match {
        case Some(account) if account.password.check(password) => Some(account)
        case None                                              => Option.empty[AccountWPassword]
      }
    }

  def sshKeysByUserName(username: String): List[SshKey] =
    db.withConnection { implicit connection =>
      SQL("""
          select ssh_key.account_id, ssh_key.public_key from ssh_key
          join account on account.id = ssh_key.account.id
          where account.username = {username}
          """)
        .on("username" -> username)
        .as(sshKeyParser.*)
    }

  def doesAccountHaveAccess(
    repositoryId: Int,
    repositoryOwner: String,
    account: Account,
    minimumLevel: AccessLevel
  ): Boolean =
    db.withConnection { implicit connection =>
      SQL("""
          select collaborator.role from collaborator
          where collaborator.account_id = {accountId}
          and collaborator.repository_id = {repositoryId}
          """)
        .on("accountId" -> account.accountId, "repositoryId" -> repositoryId)
        .as(SqlParser.int("collaborator.role").singleOpt) match {
        case Some(r)                                     => minimumLevel.role >= AccessLevel.fromRole(r).getOrElse(ViewAccess).role
        case None if repositoryOwner == account.username => true // owner have absolute access
        case None if repositoryOwner != account.username => false
      }
    }

}
