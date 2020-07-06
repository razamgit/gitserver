package models

import java.io.File

case class RzRepository(owner: String, name: String, path: File)

object RzRepository {
  val appConfig: AppConfig = AppConfig.load

  def apply(owner: String, name: String): RzRepository =
    new RzRepository(
      owner,
      name.replaceFirst("\\.wiki\\Z", ""),
      new File(RzRepository.appConfig.gitDirectory, s"$owner/$name")
    )

  /**
   * For repository name like "owner/reponame"
   */
  def apply(name: String): RzRepository =
    new RzRepository(
      name.split("/").head,
      name.replaceFirst("\\.wiki\\Z", ""),
      new File(RzRepository.appConfig.gitDirectory, name)
    )
}
