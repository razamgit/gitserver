package git

import filters.GitAuthFilter
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import models._
import org.eclipse.jgit.http.server.GitServlet
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib._
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.resolver._
import org.slf4j.LoggerFactory

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
      res.sendRedirect(BaseUrl(req).url + "/" + paths.dropRight(1).last + "/" + paths.last)
    } else {
      // response for git client
      withLockRepository(req) {
        super.service(req, res)
      }
    }
  }

  private def withLockRepository[T](req: HttpServletRequest)(f: => T): T =
    if (req.getAttribute(GitLiterals.RepositoryLockKey.toString) != null) {
      RzRepositoryLock.lock(req.getAttribute(GitLiterals.RepositoryLockKey.toString).asInstanceOf[String]) {
        f
      }
    } else {
      f
    }
}

class RzRepositoryResolver extends RepositoryResolver[HttpServletRequest] {
  override def open(req: HttpServletRequest, name: String): Repository = {
    val account = req.getAttribute(GitLiterals.UserName.toString).asInstanceOf[Account]
    new FileRepository(RzRepository(account.username, name).path)
  }

}

class RzReceivePackFactory extends ReceivePackFactory[HttpServletRequest] {

  private val logger = LoggerFactory.getLogger(classOf[RzReceivePackFactory])

  override def create(request: HttpServletRequest, db: Repository): ReceivePack = {
    val receivePack = new ReceivePack(db)
    val pusher      = request.getAttribute(GitLiterals.UserName.toString).asInstanceOf[String]

    logger.debug("requestURI: " + request.getRequestURI)
    logger.debug("pusher:" + pusher)

    val paths      = GitPaths(request).list
    val owner      = paths(1)
    val repository = paths(2).stripSuffix(".git")

    logger.debug("repository:" + owner + "/" + repository)

    val baseUrl = BaseUrl(request)
    val hook    = new CommitLogHook(owner, repository, pusher, baseUrl.url)
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
