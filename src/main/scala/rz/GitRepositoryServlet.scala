package rz

import java.io.File

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
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
 * Authentication is provided by [[GitAuthenticationFilter]].
 */
class GitRepositoryServlet extends GitServlet {

  private val logger = LoggerFactory.getLogger(classOf[GitRepositoryServlet])
//  private implicit val jsonFormats = gitbucket.core.api.JsonFormat.jsonFormats
  private val HttpProtocols = Vector("http", "https")

  override def init(config: ServletConfig): Unit = {
    setReceivePackFactory(new RzReceivePackFactory())

    setRepositoryResolver(new RzRepositoryResolver)

    super.init(config)
  }

  def parseBaseUrl(req: HttpServletRequest): String = {
    val url = req.getRequestURL.toString
    val path = req.getRequestURI
    val contextPath = req.getContextPath
    val len = url.length - path.length + contextPath.length

    val base = url.substring(0, len).stripSuffix("/")
    Option(req.getHeader("X-Forwarded-Proto"))
      .map(_.toLowerCase())
      .filter(HttpProtocols.contains)
      .fold(base)(_ + base.dropWhile(_ != ':'))
  }

  override def service(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val agent = req.getHeader("USER-AGENT")
    val index = req.getRequestURI.indexOf(".git")
    if (index >= 0 && (agent == null || agent.toLowerCase.indexOf("git") < 0)) {
      // redirect for browsers
      val paths = req.getRequestURI.substring(0, index).split("/")
      res.sendRedirect(parseBaseUrl(req) + "/" + paths.dropRight(1).last + "/" + paths.last)

    } else {
      // response for git client
      withLockRepository(req) {
        super.service(req, res)
      }
    }
  }

  private def withLockRepository[T](req: HttpServletRequest)(f: => T): T = {
    if (req.getAttribute("REPOSITORY_LOCK_KEY") != null) {
      RepoLock.lock(req.getAttribute("REPOSITORY_LOCK_KEY").asInstanceOf[String]) {
        f
      }
    } else {
      f
    }
  }
}

class RzRepositoryResolver extends RepositoryResolver[HttpServletRequest] {
  val appConfig = AppConfig.load
  override def open(req: HttpServletRequest, name: String): Repository = {
    new FileRepository(new File(appConfig.gitDirectory, name))
  }

}

class RzReceivePackFactory extends ReceivePackFactory[HttpServletRequest] {

  private val logger = LoggerFactory.getLogger(classOf[RzReceivePackFactory])

  override def create(request: HttpServletRequest, db: Repository): ReceivePack = {
    val receivePack = new ReceivePack(db)
    receivePack
  }
}


class CommitLogHook(owner: String, repository: String, pusher: String, baseUrl: String, sshUrl: Option[String])
  extends PostReceiveHook with PreReceiveHook {

  private val logger = LoggerFactory.getLogger(classOf[CommitLogHook])

  def onPreReceive(receivePack: ReceivePack, commands: java.util.Collection[ReceiveCommand]): Unit = {
    // TODO
  }

  def onPostReceive(receivePack: ReceivePack, commands: java.util.Collection[ReceiveCommand]): Unit = {
    // TODO
  }

}

