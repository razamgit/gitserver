package rz

import javax.servlet._
import javax.servlet.http._
import org.slf4j.LoggerFactory
import anorm._

/**
 * Provides BASIC Authentication for [[GitRepositoryServlet]].
 */
class GitAuthFilter extends Filter {
  val appConfig: AppConfig = AppConfig.load
  val db = new DB()

  private val logger = LoggerFactory.getLogger(classOf[GitAuthFilter])

  def init(config: FilterConfig): Unit = {}

  def destroy(): Unit = {}

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    val request = req.asInstanceOf[HttpServletRequest]
    val response = res.asInstanceOf[HttpServletResponse]

    val wrappedResponse = new HttpServletResponseWrapper(response) {
      override def setCharacterEncoding(encoding: String): Unit = {}
    }

    val isUpdating = request.getRequestURI.endsWith("/git-receive-pack") || "service=git-receive-pack".equals(
      request.getQueryString
    )
    try {
      defaultRepository(request, wrappedResponse, chain, appConfig, isUpdating)
    } catch {
      case ex: Exception =>
        logger.error("error", ex)
        Auth.requireAuth(response)
    }
  }

  private def paths(request: HttpServletRequest): Array[String] =
    (request.getRequestURI.substring(request.getContextPath.length + 1) match {
      case path if path.startsWith("api/v3/repos/") => path.substring(13 /* "/api/v3/repos".length */)
      case path if path.startsWith("api/v3/orgs/") => path.substring(12 /* "/api/v3/orgs".length */)
      case path => path
    }).split("/")

  private def defaultRepository(
                                 request: HttpServletRequest,
                                 response: HttpServletResponse,
                                 chain: FilterChain,
                                 settings: AppConfig,
                                 isUpdating: Boolean
                               ): Unit = {
    paths(request) match {
      case Array(_, repositoryOwner, repositoryName, _*) =>
        getRepository(repositoryOwner, repositoryName.replaceFirst("(\\.wiki)?\\.git$", "")) match {
          case Some(repository) =>
            logger.info(s"found a repo $repositoryOwner / $repositoryName")
            // Authentication is required
            val passed = for {
              authorizationHeader <- Option(request.getHeader("Authorization"))
              accountUsername <- authenticateByHeader(authorizationHeader, settings)
            } yield
              if (isUpdating) {
                //                if (hasDeveloperRole(repository.owner, repository.name, Some(account))) {
                request.setAttribute(RequestKeys.UserName, accountUsername)
                request.setAttribute(RequestKeys.RepositoryLockKey, s"${repositoryOwner}/${repositoryName}")
                true
                //                } else false
              } else {
                //                if (hasGuestRole(repository.owner, repository.name, Some(account))) {
                request.setAttribute(RequestKeys.UserName, accountUsername)
                true
                //                } else false
              }
            //        } else true

            val execute = passed.getOrElse(false)

            if (execute) {
              chain.doFilter(request, response)
            } else {
              Auth.requireAuth(response)
            }
          case None => {
            logger.debug(s"Repository ${repositoryOwner}/${repositoryName} is not found.")
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
          }
        }
      case _ => {
        logger.debug(s"Not enough path arguments: ${paths(request)}")
        response.sendError(HttpServletResponse.SC_NOT_FOUND)
      }
    }
  }

  /**
   * Authenticate by an Authorization header.
   * This accepts one of the following credentials:
   * - username and password
   * - username and personal access token
   *
   * @param authorizationHeader Authorization header
   * @param settings            system settings
   * @return an account or none
   */
  private def authenticateByHeader(authorizationHeader: String, settings: AppConfig): Option[String] = {
    val Array(username, password) = Auth.decodeAuthHeader(authorizationHeader).split(":", 2)
    logger.info(s"auth for $username with $password")
    if (authenticate(username, password)) {
      Some(username)
    } else {
      None
    }
  }

  case class Repository(id: Int)

  def getRepository(repositoryOwner: String, repositoryName: String): Option[Repository] = {
    db.withConnection { implicit connection =>
      val sql = SQL"""select repository.id from repository
             join account on account.id = repository.owner_id
             where account.username = '$repositoryOwner'
             and repository."name" = '$repositoryName'
             """
      sql.as(anorm.SqlParser.scalar[Int].singleOpt) match {
        case Some(id) => Some(Repository(id))
        case None => None
      }
    }
  }

  private def authenticate(username: String, password: String): Boolean

  = {
    db.withConnection { implicit connection =>
      val sql = SQL"""select password from account where account.username = $username"""
      sql.as(anorm.SqlParser.scalar[String].singleOpt) match {
        case Some(passwordHashed) => {
          logger.info(s"found a hashed pswd $passwordHashed")
          Auth.checkHash(password, passwordHashed)
        }
        case None => false
      }
    }
  }
}
