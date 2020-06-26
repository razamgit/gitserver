package models

import java.io.File

case object RepositoryDirectory {
  val appConfig: AppConfig                               = AppConfig.load
  def toFile(name: String): File                         = new File(appConfig.gitDirectory, name)
  def toFile(username: String, repository: String): File = new File(appConfig.gitDirectory, s"$username/$repository")
}

case class RepositoryName(name: String) {
  override val toString: String = name
}

object RepositoryName {
  def fromRepo(name: String): RepositoryName = this(name.replaceFirst("\\.wiki\\Z", ""))
}
