package repositories

import anorm.SqlParser.get
import anorm._
import models.{ Database, HashedString }

case class Repository(id: Long)

class RzRepository(db: Database) {

  /**
   * Parse a Hashed Password from a ResultSet
   */
  val passwordParser: RowParser[HashedString] = {
    get[String]("account.password").map(password => HashedString(password))
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
}
