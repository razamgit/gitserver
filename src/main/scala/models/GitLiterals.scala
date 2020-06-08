package models

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
}
