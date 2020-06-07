package models

import java.sql.Connection

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import services.PgConfig

class Database(pgConfig: PgConfig) {

  private val dataSource: HikariDataSource = {
    val config = new HikariConfig()
    config.setDriverClassName(PgConfig.pgDriver)
    config.setJdbcUrl(s"${PgConfig.jdbcPgPrefix}${pgConfig.host}:${pgConfig.port}/${pgConfig.dbname}")
    config.setUsername(pgConfig.user)
    config.setPassword(pgConfig.password)
    config.setAutoCommit(false)
    config.setMaximumPoolSize(pgConfig.maxPoolSize)
    config.setReadOnly(true)

    new HikariDataSource(config)
  }

  /**
   * Execute a block of code, providing a JDBC connection. The connection and all created statements are
   * automatically released.
   *
   * @param block Code block to execute.
   */
  def withConnection[A](block: Connection => A): A = {
    val connection = dataSource.getConnection()
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }
}
