package git

import java.io.File

import javax.servlet.ServletConfig
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.eclipse.jgit.http.server.GitServlet
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib._
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.resolver._
import org.slf4j.LoggerFactory
import services.{ AppConfig, GitHttpService, RepositoryLockService }

/**
 * Provides Git repository via HTTP.
 *
 * This servlet provides only Git repository functionality.
 * Authentication is provided by [[GitAuthFilter]].
 */
class GitRepositoryServlet extends GitServlet {
  private val logger = LoggerFactory.getLogger(classOf[GitRepositoryServlet])

  override def init(config: ServletConfig): Unit = {
    setReceivePackFactory(new RzReceivePackFactory())
    setRepositoryResolver(new RzRepositoryResolver)
    super.init(config)
  }

  override def service(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val agent = req.getHeader("USER-AGENT")
    val index = req.getRequestURI.indexOf(".git")
    if (index >= 0 && (agent == null || agent.toLowerCase.indexOf("git") < 0)) {
      // redirect for browsers
      val paths = req.getRequestURI.substring(0, index).split("/")
      res.sendRedirect(GitHttpService.parseBaseUrl(req) + "/" + paths.dropRight(1).last + "/" + paths.last)
    } else {
      // response for git client
      withLockRepository(req) {
        super.service(req, res)
      }
    }
  }

  private def withLockRepository[T](req: HttpServletRequest)(f: => T): T =
    if (req.getAttribute(GitRepositoryServlet.RepositoryLockKey) != null) {
      RepositoryLockService.lock(req.getAttribute(GitRepositoryServlet.RepositoryLockKey).asInstanceOf[String]) {
        f
      }
    } else {
      f
    }
}

object GitRepositoryServlet {

  /**
   * Request key for the username which is used during Git repository access.
   */
  val UserName = "USER_NAME"

  /**
   * Request key for the Lock key which is used during Git repository write access.
   */
  val RepositoryLockKey = "REPOSITORY_LOCK_KEY"
}

class RzRepositoryResolver extends RepositoryResolver[HttpServletRequest] {
  val appConfig: AppConfig = AppConfig.load

  override def open(req: HttpServletRequest, name: String): Repository =
    new FileRepository(new File(appConfig.gitDirectory, name))

}

class RzReceivePackFactory extends ReceivePackFactory[HttpServletRequest] {

  private val logger = LoggerFactory.getLogger(classOf[RzReceivePackFactory])

  override def create(request: HttpServletRequest, db: Repository): ReceivePack = {
    val receivePack = new ReceivePack(db)
    val pusher      = request.getAttribute(GitRepositoryServlet.UserName).asInstanceOf[String]

    logger.debug("requestURI: " + request.getRequestURI)
    logger.debug("pusher:" + pusher)

    val paths      = GitHttpService.paths(request)
    val owner      = paths(1)
    val repository = paths(2).stripSuffix(".git")

    logger.debug("repository:" + owner + "/" + repository)

    val baseUrl = GitHttpService.parseBaseUrl(request)
    val hook    = new CommitLogHook(owner, repository, pusher, baseUrl)
    receivePack.setPreReceiveHook(hook)
    receivePack.setPostReceiveHook(hook)

    receivePack
  }
}

class CommitLogHook(owner: String, repository: String, pusher: String, baseUrl: String)
    extends PostReceiveHook
    with PreReceiveHook {

  private val logger = LoggerFactory.getLogger(classOf[CommitLogHook])

  def onPreReceive(receivePack: ReceivePack, commands: java.util.Collection[ReceiveCommand]): Unit = {
    // TODO
  }

  def onPostReceive(receivePack: ReceivePack, commands: java.util.Collection[ReceiveCommand]): Unit = {
    // TODO
  }

}
