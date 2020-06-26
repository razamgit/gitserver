package filters

import git.GitRepositoryServlet
import javax.servlet._
import javax.servlet.http._
import models._
import org.slf4j.LoggerFactory
import repositories.RzRepository

/**
 * Provides BASIC Authentication for [[GitRepositoryServlet]].
 */
class GitAuthFilter extends Filter {
  val appConfig: AppConfig = AppConfig.load
  val db                   = new Database(appConfig.db)
  val rzRepository         = new RzRepository(db)

  private val logger = LoggerFactory.getLogger(classOf[GitAuthFilter])

  def init(config: FilterConfig): Unit = {}

  def destroy(): Unit = {}

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
        rzRepository.getRepository(repositoryOwner, repositoryName.replaceFirst("(\\.wiki)?\\.git$", "")) match {
          case Some(repository) =>
            // Authentication is required
            val passed = for {
              authorizationHeader <- Option(request.getHeader("Authorization"))
              accountUsername     <- authenticateByHeader(authorizationHeader, settings)
            } yield
              if (isUpdating) {
                request.setAttribute(GitLiterals.UserName.toString, accountUsername)
                request.setAttribute(GitLiterals.RepositoryLockKey.toString, s"$repositoryOwner/$repositoryName")
                true
              } else {
                request.setAttribute(GitLiterals.UserName.toString, accountUsername)
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
  private def authenticateByHeader(authorizationHeader: String, settings: AppConfig): Option[String] = {
    val header = AuthorizationHeader(authorizationHeader)
    header match {
      case Some(header) if rzRepository.isUserWithPasswordExists(header.username, header.password) =>
        Some(header.username)
      case _ => Option.empty[String]
    }
  }
}
