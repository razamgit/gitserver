package rz

import com.typesafe.config.ConfigFactory

sealed trait AppEnvironment

case object Development extends AppEnvironment

case object Production extends AppEnvironment

object AppEnvironment {
  def fromString(s: String): AppEnvironment = {
    s match {
      case "development" => Development
      case "production" => Production
    }
  }

  def asString(s: AppEnvironment): String = {
    s match {
      case Development => "development"
      case Production => "production"
    }
  }
}

case class AppConfig(
                      port: Int,
                      webBase: String,
                      gitDirectory: String,
                      env: AppEnvironment) {

  def isProduction: Boolean = env == Production

  def isDevelopment: Boolean = env == Development
}


object AppConfig {
  def load: AppConfig = {
    val cfg = ConfigFactory.load

    val gitDirectory = cfg.getString("gitDirectory")
    val webBase = cfg.getString("base")
    val port = cfg.getInt("port")
    val env = AppEnvironment.fromString(cfg.getString("environment"))

    AppConfig(port, webBase, gitDirectory, env)
  }
}

