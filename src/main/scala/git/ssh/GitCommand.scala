package git.ssh

import java.io.{InputStream, OutputStream}

import filters.RzPublickeyAuthenticator
import git.CommitLogHook
import models.GitLiterals.GitCommandRegex
import models._
import org.apache.sshd.server.command.{Command, CommandFactory}
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.shell.UnknownCommand
import org.apache.sshd.server.{Environment, ExitCallback, SessionAware}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.{ReceivePack, UploadPack}
import org.slf4j.LoggerFactory
import repositories.RzEntitiesRepository

import scala.util.Using


abstract class GitCommand(val owner: String, val repoName: String) extends Command with SessionAware {

  private val logger = LoggerFactory.getLogger(classOf[GitCommand])

  @volatile protected var err: OutputStream       = _
  @volatile protected var in: InputStream         = _
  @volatile protected var out: OutputStream       = _
  @volatile protected var callback: ExitCallback  = _
  @volatile private var authType: Option[Account] = None

  protected def runTask(authType: Account): Unit

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
    this.authType = RzPublickeyAuthenticator.getAuthType(serverSession)

}

class DefaultGitUploadPack(db: Database, owner: String, repoName: String) extends GitCommand(owner, repoName) {
  val rzRepository = new RzEntitiesRepository(db)

  def upload(repository: RzRepository): Unit =
    Using.resource(Git.open(repository.path)) { git =>
      val repository = git.getRepository
      val upload     = new UploadPack(repository)
      upload.upload(in, out, err)
    }

  override protected def runTask(account: Account): Unit = {
    val repository = RzRepository(owner, repoName)
    rzRepository.repositoryId(owner, repository.name) match {
      case Some(id) if rzRepository.doesAccountHaveAccess(id, owner, account, ViewAccess) => upload(repository)
      case _                                                                                 => ()
    }
  }
}

class DefaultGitReceivePack(db: Database, owner: String, repoName: String, baseUrl: String)
  extends GitCommand(owner, repoName) {
  val rzRepository = new RzEntitiesRepository(db)

  private def receivePack(repository: RzRepository, account: Account): Unit =
    Using.resource(Git.open(repository.path)) { git =>
      val repository = git.getRepository
      val receive    = new ReceivePack(repository)
      val hook       = new CommitLogHook(owner, repoName, account.username, baseUrl)
      receive.setPreReceiveHook(hook)
      receive.setPostReceiveHook(hook)
      receive.receive(in, out, err)
    }

  override protected def runTask(account: Account): Unit = {
    val repository = RzRepository(owner, repoName)
    rzRepository.repositoryId(owner, repository.name) match {
      case Some(id) if rzRepository.doesAccountHaveAccess(id, owner, account, EditAccess) =>
        receivePack(repository, account)
      case _ => ()
    }
  }
}

class GitCommandFactory(db: Database, baseUrl: String) extends CommandFactory {
  private val logger = LoggerFactory.getLogger(classOf[GitCommandFactory])

  override def createCommand(command: String): Command = {
    logger.debug(s"command: $command")

    command match {
      case GitCommandRegex.toRegex("upload", owner, repoName) => new DefaultGitUploadPack(db, owner, repoName)
      case GitCommandRegex.toRegex("receive", owner, repoName) =>
        new DefaultGitReceivePack(db, owner, repoName, baseUrl)
      case _ => new UnknownCommand(command)
    }
  }

}
