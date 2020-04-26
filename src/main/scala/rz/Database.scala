package rz

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.servlet.ServletRequest
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.{Session, Database => SlickDatabase}

object Database {

  private val dataSource: HikariDataSource = {
    val config = new HikariConfig()
    config.setDriverClassName("org.postgresql.Driver2")
    config.setJdbcUrl("jdbc:postgresql://localhost/razam")
    config.setUsername("razam")
    config.setPassword("razam")
    config.setAutoCommit(false)

    new HikariDataSource(config)
  }

  private val db: SlickDatabase = {
    SlickDatabase.forDataSource(dataSource, Some(dataSource.getMaximumPoolSize))
  }

  def withSession(): JdbcBackend.Session = db.createSession()

  def getSession(req: ServletRequest): Session = req.getAttribute("DB_SESSION").asInstanceOf[Session]

  def closeDataSource(): Unit = dataSource.close()

}