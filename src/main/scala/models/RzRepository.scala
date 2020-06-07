package models

import anorm._
import services.EncryptionService

case class Repository(id: Int)

class RzRepository(db: Database) {

  def getRepository(repositoryOwner: String, repositoryName: String): Option[Repository] =
    db.withConnection { implicit connection =>
      val sql = SQL("""select repository.id from repository
             join account on account.id = repository.owner_id
             where account.username = {repositoryOwner}
             and repository."name" = {repositoryName}
             """).on("repositoryOwner" -> repositoryOwner, "repositoryName" -> repositoryName)
      sql.as(anorm.SqlParser.scalar[Int].singleOpt) match {
        case Some(id) => Some(Repository(id))
        case None     => None
      }
    }

  def isUserExists(username: String, password: String): Boolean =
    db.withConnection { implicit connection =>
      val sql = SQL("select password from account where account.username = {username}").on("username" -> username)
      sql.as(anorm.SqlParser.scalar[String].singleOpt) match {
        case Some(passwordHashed) =>
          EncryptionService.checkHash(password, passwordHashed)
        case None => false
      }
    }
}
