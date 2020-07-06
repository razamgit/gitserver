package filters

import git.GitRepositoryServlet
import javax.servlet._
import javax.servlet.http._
import models._
import org.slf4j.LoggerFactory
import repositories.RzEntitiesRepository

/**
 * Provides BASIC Authentication for [[GitRepositoryServlet]].
 */
class GitAuthFilter extends Filter {
  val appConfig: AppConfig = AppConfig.load
  val db                   = new Database(appConfig.db)
  val rzRepository         = new RzEntitiesRepository(db)

  private val logger = LoggerFactory.getLogger(classOf[GitAuthFilter])

  override def init(config: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  def requireAuth(response: HttpServletResponse): Unit = {
    response.setHeader("WWW-Authenticate", "BASIC realm=\"razam\"")
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    val request  = req.asInstanceOf[HttpServletRequest]
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
        logger.error("Exception", ex)
        requireAuth(response)
    }
  }

  private def defaultRepository(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain,
    settings: AppConfig,
    isUpdating: Boolean
  ): Unit =
    GitPaths(request).list match {
      case Array(_, repositoryOwner, repositoryName, _*) =>
        rzRepository.repositoryId(repositoryOwner, repositoryName.replaceFirst("(\\.wiki)?\\.git$", "")) match {
          case Some(_) =>
            // Authentication is required
            val passed = for {
              authorizationHeader <- Option(request.getHeader("Authorization"))
              account             <- authenticateByHeader(authorizationHeader, settings)
            } yield
              if (isUpdating) {
                request.setAttribute(GitLiterals.UserName.toString, account)
                request.setAttribute(GitLiterals.RepositoryLockKey.toString, s"$repositoryOwner/$repositoryName")
                true
              } else {
                request.setAttribute(GitLiterals.UserName.toString, account)
                true
              }
            val execute = passed.getOrElse(false)

            if (execute) {
              chain.doFilter(request, response)
            } else {
              requireAuth(response)
            }
          case None => response.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
      case _ => response.sendError(HttpServletResponse.SC_NOT_FOUND)
    }

  /**
   * Authenticate by an Authorization header.
   * This accepts one of the following credentials:
   * - username and password
   *
   * @param authorizationHeader Authorization header
   * @param settings            system settings
   * @return an account or none
   */
  private def authenticateByHeader(authorizationHeader: String, settings: AppConfig): Option[Account] = {
    val header = AuthorizationHeader(authorizationHeader)
    header match {
      case Some(header) => rzRepository.userWithPassword(header.username, header.password)
      case _            => Option.empty[Account]
    }
  }
}
