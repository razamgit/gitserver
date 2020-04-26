package rz

import javax.servlet._
import javax.servlet.http._
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend

/**
 * Provides BASIC Authentication for [[GitRepositoryServlet]].
 */
class GitAuthFilter extends Filter {
  val appConfig: AppConfig = AppConfig.load

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

  private def paths(request:HttpServletRequest): Array[String] =
    (request.getRequestURI.substring(request.getContextPath.length + 1) match {
      case path if path.startsWith("api/v3/repos/") => path.substring(13 /* "/api/v3/repos".length */ )
      case path if path.startsWith("api/v3/orgs/")  => path.substring(12 /* "/api/v3/orgs".length */ )
      case path                                     => path
    }).split("/")

  private def defaultRepository(
                                 request: HttpServletRequest,
                                 response: HttpServletResponse,
                                 chain: FilterChain,
                                 settings: AppConfig,
                                 isUpdating: Boolean
                               ): Unit = {
    val action = paths(request) match {
      case Array(_, repositoryOwner, repositoryName, _*) =>
        Database.withSession() { implicit session: JdbcBackend.Session  =>
          getRepository(repositoryOwner, repositoryName.replaceFirst("(\\.wiki)?\\.git$", "")) match {
            case Some(repository) => {
              val execute = if (!isUpdating && !repository.repository.isPrivate) {
                // Authentication is not required
                true
              } else {
                // Authentication is required
                val passed = for {
                  authorizationHeader <- Option(request.getHeader("Authorization"))
                  account <- authenticateByHeader(authorizationHeader, settings)
                } yield
                  if (isUpdating) {
                    if (hasDeveloperRole(repository.owner, repository.name, Some(account))) {
                      request.setAttribute(Keys.Request.UserName, account.userName)
                      request.setAttribute(Keys.Request.RepositoryLockKey, s"${repository.owner}/${repository.name}")
                      true
                    } else false
                  } else if (repository.repository.isPrivate) {
                    if (hasGuestRole(repository.owner, repository.name, Some(account))) {
                      request.setAttribute(Keys.Request.UserName, account.userName)
                      true
                    } else false
                  } else true
                passed.getOrElse(false)
              }

              if (execute) { () =>
                chain.doFilter(request, response)
              } else { () =>
                Auth.requireAuth(response)
              }
            }
            case None =>
              () =>
              {
                logger.debug(s"Repository ${repositoryOwner}/${repositoryName} is not found.")
                response.sendError(HttpServletResponse.SC_NOT_FOUND)
              }
          }
        }
      case _ =>
        () => response.sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    action()
  }
  type Session = slick.jdbc.JdbcBackend#Session

  /**
   * Authenticate by an Authorization header.
   * This accepts one of the following credentials:
   * - username and password
   * - username and personal access token
   *
   * @param authorizationHeader Authorization header
   * @param settings system settings
   * @param s database session
   * @return an account or none
   */
  private def authenticateByHeader(authorizationHeader: String, settings: AppConfig)(
    implicit s: Session
  ): Option[Account] = {
    val Array(username, password) = Auth.decodeAuthHeader(authorizationHeader).split(":", 2)
    authenticate(settings, username, password)
    }
}
