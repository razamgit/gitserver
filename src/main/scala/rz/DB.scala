package rz

import java.sql.Connection

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
 * Lots of this is stolen from Play.
 */
class DB {

  private val dataSource: HikariDataSource = {
    val config = new HikariConfig()
    config.setDriverClassName("org.postgresql.Driver")
    config.setJdbcUrl("jdbc:postgresql://localhost/razam")
    config.setUsername("razam")
    config.setPassword("razam")
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    config.setAutoCommit(false)

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
