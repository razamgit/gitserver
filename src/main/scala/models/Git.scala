package models

import javax.servlet.http.HttpServletRequest
import scala.util.matching.Regex

sealed trait GitUrl {
  def url: String
  def length: Int               = url.length
  override def toString: String = url
}

object GitUrl {
  case object ApiV3Repos extends GitUrl {
    val url: String = "api/v3/repos/"
  }
  case object ApiV3Orgs extends GitUrl {
    val url: String = "api/v3/orgs/"
  }
}

case class GitPaths(list: Array[String])

object GitPaths {
  def apply(request: HttpServletRequest): GitPaths =
    GitPaths((request.getRequestURI.substring(request.getContextPath.length + 1) match {
      case path if path.startsWith(GitUrl.ApiV3Repos.url) => path.substring(GitUrl.ApiV3Repos.length)
      case path if path.startsWith(GitUrl.ApiV3Orgs.url)  => path.substring(GitUrl.ApiV3Orgs.length)
      case path                                           => path
    }).split("/"))
}

sealed trait GitLiterals

object GitLiterals {

  /**
   * Request key for the username which is used during Git repository access.
   */
  case object UserName extends GitLiterals {
    override def toString: String = "USER_NAME"
  }

  /**
   * Request key for the Lock key which is used during Git repository write access.
   */
  case object RepositoryLockKey extends GitLiterals {
    override def toString: String = "REPOSITORY_LOCK_KEY"
  }

  case object GitCommandRegex extends GitLiterals {
    val toRegex: Regex = """\Agit-(upload|receive)-pack '/([a-zA-Z0-9\-_.]+)/([a-zA-Z0-9\-\+_.]+).git'\Z""".r
  }
}
