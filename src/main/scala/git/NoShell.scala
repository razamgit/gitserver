package git

import java.io.{ InputStream, OutputStream }

import models.SshAddress
import org.apache.sshd.common.Factory
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.{ Environment, ExitCallback }
import org.eclipse.jgit.lib.Constants

class NoShell(sshAddress: SshAddress) extends Factory[Command] {
  override def create(): Command = new Command() {
    private var in: InputStream        = _
    private var out: OutputStream      = _
    private var err: OutputStream      = _
    private var callback: ExitCallback = _

    override def start(env: Environment): Unit = {
      val message =
        """
          | Successfully SSH Access.
          | But interactive shell is disabled.
        """.stripMargin.replace("\n", "\r\n") + "\r\n"
      err.write(Constants.encode(message))
      err.flush()
      in.close()
      out.close()
      err.close()
      callback.onExit(127)
    }

    override def destroy(): Unit = {}

    override def setInputStream(in: InputStream): Unit =
      this.in = in

    override def setOutputStream(out: OutputStream): Unit =
      this.out = out

    override def setErrorStream(err: OutputStream): Unit =
      this.err = err

    override def setExitCallback(callback: ExitCallback): Unit =
      this.callback = callback
  }
}
