package rz

import scala.io.{ Source }

/**
 * Read the service information from the account's pg_service.conf
 * and pgpass files.
 *
 * It assumes they're are ~/.pg_service.conf and ~/.pgpass.
 *
 */
case class PgService(service: String,
                     host: String,
                     port: Int,
                     dbname: String,
                     user: String,
                     password: String)

//object PgService {
//
//  /**
//   *  Get the details for the named service from the combination of
//   *  the service source (svcFile) and the password source (pwdFile).
//   */
//  def apply(service: String, svcFile: Source, pwdFile: Source): PgService = {
//
//    def getService: Map[String, String] = {
//      // Suck in the services file, get rid of services before the one
//      // we want, take the one we want, turn it into a map of key-value
//      // pairs.
//      val allSvc = svcFile.getLines.toList
//      val dropLeadingSvc = allSvc.dropWhile(line ⇒ line != s"[$service]")
//        .dropWhile(line ⇒ line == s"[$service]")
//      val svcDef = dropLeadingSvc.takeWhile(line ⇒ line.matches("[^=]+=[^=]+"))
//      val svcDef2 = svcDef.map(line ⇒ line.split('=')).map(a ⇒ (a(0), a(1)))
//      svcDef2.toMap
//    }
//
//    val props = getService
//    if (props.isEmpty) {
//      throw new Exception(s"Unable to find a service configuration for $service")
//    }
//    val host = props("host")
//    val port = props("port").toInt
//    val dbname = props("dbname")
//    val user = props("user")
//
//    val pwCandidates = pwdFile.getLines.toList
//      .filter(_.matches(s"^([^:]*:){0}($host|\\*):.*")) // matches host
//      .filter(_.matches(s"^([^:]*:){1}($port|\\*):.*")) // matches port
//      .filter(_.matches(s"^([^:]*:){2}($dbname|\\*):.*")) // matches dbname
//      .filter(_.matches(s"^([^:]*:){3}($user|\\*):.*")) // matches user
//
//    if (pwCandidates.isEmpty) {
//      throw new Exception(s"Unable to find a password for $host:$port:$dbname:$user")
//    }
//    val password = pwCandidates.headOption.map(_.split(':')(4)).getOrElse("")
//    new PgService(service, host, port, dbname, user, password)
//  }
//
//  /**
//   * Get the details for the named service from the default config
//   * files (~/.pg_service.conf and ~/.pgpass).
//   */
//  def apply(service: String): PgService =
//    apply(service,
//      Source.fromFile(sys.env("HOME") + "/.pg_service.conf"),
//      Source.fromFile(sys.env("HOME") + "/.pgpass"))
//}