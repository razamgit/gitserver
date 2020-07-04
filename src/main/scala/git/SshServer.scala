package git

import java.util.concurrent.atomic.AtomicBoolean

import filters.RzPublickeyAuthenticator
import models.{ AppConfig, Database }
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.slf4j.LoggerFactory

class SshServer(db: Database) {
  private val logger = LoggerFactory.getLogger(classOf[SshServer])
  private val server = org.apache.sshd.server.SshServer.setUpDefaultServer()
  private val active = new AtomicBoolean(false)

  private def configure(settings: AppConfig): Unit = {
    server.setPort(settings.ssh.port)
    val provider = new SimpleGeneratorHostKeyProvider(
      java.nio.file.Paths.get(s"${settings.keysDirectory}/rz.key")
    )
    provider.setAlgorithm("RSA")
    provider.setOverwriteAllowed(false)
    server.setKeyPairProvider(provider)
    server.setPublickeyAuthenticator(new RzPublickeyAuthenticator(db))
    server.setCommandFactory(new GitCommandFactory(db, settings.webBase))
    server.setShellFactory(new NoShell(settings.ssh))
  }

  def start(settings: AppConfig): Unit =
    if (active.compareAndSet(false, true)) {
      configure(settings)
      server.start()
      logger.info(s"SSH Server is listening on ${server.getPort}")
    }

  def stop(): Unit =
    if (active.compareAndSet(true, false)) {
      server.stop(true)
      logger.info("SSH Server has been stopped")
    }

  def isActive: Boolean = active.get
}
