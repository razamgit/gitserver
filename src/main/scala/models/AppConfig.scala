package models

import com.typesafe.config.ConfigFactory

case class PgConfig(host: String, port: Int, dbname: String, user: String, password: String, maxPoolSize: Int)

object PgConfig {
  val pgDriver     = "org.postgresql.Driver"
  val jdbcPgPrefix = "jdbc:postgresql://"
}

case class AppConfig(port: Int, webBase: String, gitDirectory: String, db: PgConfig)

object AppConfig {
  def load: AppConfig = {
    val cfg = ConfigFactory.load

    val gitDirectory = cfg.getString("git.path")
    val webBase      = cfg.getString("http.base")
    val port         = cfg.getInt("http.port")

    val db = PgConfig(
      cfg.getString("db.serverName"),
      cfg.getInt("db.portNumber"),
      cfg.getString("db.databaseName"),
      cfg.getString("db.user"),
      cfg.getString("db.password"),
      cfg.getInt("db.maxPoolSize")
    )

    AppConfig(port, webBase, gitDirectory, db)
  }
}
