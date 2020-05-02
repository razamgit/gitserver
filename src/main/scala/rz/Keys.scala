package rz

/**
 * Define request keys.
 */
object RequestKeys {

  /**
   * Request key for the Slick Session.
   */
  val DBSession = "DB_SESSION"

  /**
   * Request key for the Ajax request flag.
   */
  val Ajax = "AJAX"

  /**
   * Request key for the /api/v3 request flag.
   */
  val APIv3 = "APIv3"

  /**
   * Request key for the username which is used during Git repository access.
   */
  val UserName = "USER_NAME"

  /**
   * Request key for the Lock key which is used during Git repository write access.
   */
  val RepositoryLockKey = "REPOSITORY_LOCK_KEY"

  /**
   * Generate request key for the request cache.
   */
  def Cache(key: String) = s"cache.${key}"

}