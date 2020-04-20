package rz

import java.io.File
import java.util
import java.util.Date

import scala.util.Using

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.http.server.GitServlet
import org.eclipse.jgit.lib._
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.resolver._
import org.slf4j.LoggerFactory
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jgit.diff.DiffEntry.ChangeType
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.json4s.jackson.Serialization._

/**
 * Provides Git repository via HTTP.
 *
 * This servlet provides only Git repository functionality.
 * Authentication is provided by [[GitAuthenticationFilter]].
 */
class GitRepositoryServlet extends GitServlet with SystemSettingsService {

  private val logger = LoggerFactory.getLogger(classOf[GitRepositoryServlet])
  private implicit val jsonFormats = gitbucket.core.api.JsonFormat.jsonFormats

  override def init(config: ServletConfig): Unit = {
    setReceivePackFactory(new RzReceivePackFactory())

    val root: File = new File(Directory.RepositoryHome)
    setRepositoryResolver(new RzRepositoryResolver)

    super.init(config)
  }

  override def service(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val agent = req.getHeader("USER-AGENT")
    val index = req.getRequestURI.indexOf(".git")
    if (index >= 0 && (agent == null || agent.toLowerCase.indexOf("git") < 0)) {
      // redirect for browsers
      val paths = req.getRequestURI.substring(0, index).split("/")
      res.sendRedirect(baseUrl(req) + "/" + paths.dropRight(1).last + "/" + paths.last)

    } else if (req.getMethod.toUpperCase == "POST" && req.getRequestURI.endsWith("/info/lfs/objects/batch")) {
      withLockRepository(req) {
        serviceGitLfsBatchAPI(req, res)
      }
    } else {
      // response for git client
      withLockRepository(req) {
        super.service(req, res)
      }
    }
  }

  private def withLockRepository[T](req: HttpServletRequest)(f: => T): T = {
    if (req.hasAttribute(Keys.Request.RepositoryLockKey)) {
      LockUtil.lock(req.getAttribute(Keys.Request.RepositoryLockKey).asInstanceOf[String]) {
        f
      }
    } else {
      f
    }
  }
}

class RzRepositoryResolver extends RepositoryResolver[HttpServletRequest] {

  override def open(req: HttpServletRequest, name: String): Repository = {
    // Rewrite repository path if routing is marched
    PluginRegistry()
      .getRepositoryRouting("/" + name)
      .map {
        case GitRepositoryRouting(urlPattern, localPath, _) =>
          val path = urlPattern.r.replaceFirstIn(name, localPath)
          new FileRepository(new File(Directory.GitBucketHome, path))
      }
      .getOrElse {
        new FileRepository(new File(Directory.RepositoryHome, name))
      }
  }

}

class RzReceivePackFactory extends ReceivePackFactory[HttpServletRequest] with SystemSettingsService {

  private val logger = LoggerFactory.getLogger(classOf[RzReceivePackFactory])

  override def create(request: HttpServletRequest, db: Repository): ReceivePack = {
    val receivePack = new ReceivePack(db)

    if (PluginRegistry().getRepositoryRouting(request.gitRepositoryPath).isEmpty) {
      val pusher = request.getAttribute(Keys.Request.UserName).asInstanceOf[String]

      logger.debug("requestURI: " + request.getRequestURI)
      logger.debug("pusher:" + pusher)

      defining(request.paths) { paths =>
        val owner = paths(1)
        val repository = paths(2).stripSuffix(".git")

        logger.debug("repository:" + owner + "/" + repository)

        val settings = loadSystemSettings()
        val baseUrl = settings.baseUrl(request)
        val sshUrl = settings.sshAddress.map { x =>
          s"${x.genericUser}@${x.host}:${x.port}"
        }
      }
    }

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

