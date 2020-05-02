import java.util

import javax.servlet.{DispatcherType, ServletContext}
import org.scalatra._
import rz._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    val appConfig = AppConfig.load

    val dir = new java.io.File(appConfig.gitDirectory)
    if (!dir.exists) {
      dir.mkdirs()
    }

    context.addFilter("gitAuthenticationFilter", new GitAuthFilter)
    context
      .getFilterRegistration("gitAuthenticationFilter")
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/git/*")
  }
}
