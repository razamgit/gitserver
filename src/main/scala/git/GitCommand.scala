package git

import java.io.{ InputStream, OutputStream }

import filters.PublicKeyAuthenticator
import models.{ AuthType, Database, RepositoryDirectory, RepositoryName }
import org.apache.sshd.server.command.{ Command, CommandFactory }
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.shell.UnknownCommand
import org.apache.sshd.server.{ Environment, ExitCallback, SessionAware }
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.{ ReceivePack, UploadPack }
import org.slf4j.LoggerFactory
import repositories.RzRepository

import scala.util.Using
import scala.util.matching.Regex

object GitCommand {
  val DefaultCommandRegex: Regex = """\Agit-(upload|receive)-pack '/([a-zA-Z0-9\-_.]+)/([a-zA-Z0-9\-\+_.]+).git'\Z""".r
  val SimpleCommandRegex: Regex  = """\Agit-(upload|receive)-pack '/(.+\.git)'\Z""".r
}

abstract class GitCommand extends Command with SessionAware {

  private val logger = LoggerFactory.getLogger(classOf[GitCommand])

  @volatile protected var err: OutputStream        = _
  @volatile protected var in: InputStream          = _
  @volatile protected var out: OutputStream        = _
  @volatile protected var callback: ExitCallback   = _
  @volatile private var authType: Option[AuthType] = None

  protected def runTask(authType: AuthType): Unit

  private def newTask(): Runnable = () => {
    authType match {
      case Some(authType) =>
        try {
          runTask(authType)
          callback.onExit(0)
        } catch {
          case e: RepositoryNotFoundException =>
            logger.info(e.getMessage)
            callback.onExit(1, "Repository Not Found")
          case e: Throwable =>
            logger.error(e.getMessage, e)
            callback.onExit(1)
        }
      case None =>
        val message = "User not authenticated"
        logger.error(message)
        callback.onExit(1, message)
    }
  }

  final override def start(env: Environment): Unit = {
    val thread = new Thread(newTask())
    thread.start()
  }

  override def destroy(): Unit = {}

  override def setExitCallback(callback: ExitCallback): Unit =
    this.callback = callback

  override def setErrorStream(err: OutputStream): Unit =
    this.err = err

  override def setOutputStream(out: OutputStream): Unit =
    this.out = out

  override def setInputStream(in: InputStream): Unit =
    this.in = in

  override def setSession(serverSession: ServerSession): Unit =
    this.authType = PublicKeyAuthenticator.getAuthType(serverSession)

}

abstract class DefaultGitCommand(val owner: String, val repoName: String) extends GitCommand {

  protected def userName(authType: AuthType): String =
    authType match {
      case AuthType.UserAuthType(userName) => userName
      case AuthType.DeployKeyType(_)       => owner
    }
}

class DefaultGitUploadPack(db: Database, owner: String, repoName: String) extends DefaultGitCommand(owner, repoName) {
  val rzRepository = new RzRepository(db)

  override protected def runTask(authType: AuthType): Unit = {
    val repoNameCleared = RepositoryName.fromRepo(repoName)
    rzRepository.getRepository(owner, repoNameCleared.toString) match {
      case Some(repository) =>
        Using.resource(Git.open(RepositoryDirectory.toFile(owner, repoNameCleared.toString))) { git =>
          val repository = git.getRepository
          val upload     = new UploadPack(repository)
          upload.upload(in, out, err)
        }
      case _ => ()
    }
  }
}

class DefaultGitReceivePack(db: Database, owner: String, repoName: String, baseUrl: String)
    extends DefaultGitCommand(owner, repoName) {
  val rzRepository = new RzRepository(db)

  override protected def runTask(authType: AuthType): Unit = {
    val repoNameCleared = RepositoryName.fromRepo(repoName)

    rzRepository.getRepository(owner, repoNameCleared.toString) match {
      case Some(repository) =>
        Using.resource(Git.open(RepositoryDirectory.toFile(owner, repoNameCleared.toString))) { git =>
          val repository = git.getRepository
          val receive    = new ReceivePack(repository)
          val hook       = new CommitLogHook(owner, repoName, userName(authType), baseUrl)
          receive.setPreReceiveHook(hook)
          receive.setPostReceiveHook(hook)
          receive.receive(in, out, err)
        }
      case _ => ()
    }
  }
}

class GitCommandFactory(db: Database, baseUrl: String) extends CommandFactory {
  private val logger = LoggerFactory.getLogger(classOf[GitCommandFactory])

  override def createCommand(command: String): Command = {
    logger.debug(s"command: $command")

    command match {
      case GitCommand.DefaultCommandRegex("upload", owner, repoName) => new DefaultGitUploadPack(db, owner, repoName)
      case GitCommand.DefaultCommandRegex("receive", owner, repoName) =>
        new DefaultGitReceivePack(db, owner, repoName, baseUrl)
      case _ => new UnknownCommand(command)
    }
  }

}
