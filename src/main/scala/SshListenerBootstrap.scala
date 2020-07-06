import git.ssh.SshServer
import javax.servlet.{ServletContextEvent, ServletContextListener}
import models.{AppConfig, Database}
import org.slf4j.LoggerFactory

/*
 * Start a SSH Server Daemon
 *
 * How to use:
 * git clone ssh://username@host_or_ip:2200/owner/repository_name.git
 */
class SshListenerBootstrap extends ServletContextListener {

  private val logger = LoggerFactory.getLogger(classOf[SshListenerBootstrap])

  val settings: AppConfig = AppConfig.load

  val dir = new java.io.File(settings.keysDirectory)
  if (!dir.exists) {
    dir.mkdirs()
  }

  val db: Database = new Database(settings.db)
  val sshServer    = new SshServer(db)

  override def contextInitialized(sce: ServletContextEvent): Unit =
    sshServer.start(settings)

  override def contextDestroyed(sce: ServletContextEvent): Unit =
    sshServer.stop()

}
