import java.util

import filters.GitAuthFilter
import javax.servlet.{ DispatcherType, ServletContext }
import org.scalatra._
import git._
import models.AppConfig

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    val settings = AppConfig.load

    val dir = new java.io.File(settings.gitDirectory)
    if (!dir.exists) {
      dir.mkdirs()
    }

    context.addFilter("gitAuthenticationFilter", new GitAuthFilter)
    context
      .getFilterRegistration("gitAuthenticationFilter")
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/git/*")
  }
}
