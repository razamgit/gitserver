import rz._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    val appConfig = AppConfig.load

    val dir = new java.io.File(appConfig.gitDirectory)
    if (!dir.exists) {
      dir.mkdirs()
    }
  }
}
