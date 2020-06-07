package services

import javax.servlet.http.HttpServletRequest

/**
 * Define request keys.
 */
object GitHttpService {
  private val HttpProtocols = Vector("http", "https")

  val apiv3repos = "api/v3/repos/"
  val apiv3Orgs  = "api/v3/orgs/"

  def paths(request: HttpServletRequest): Array[String] =
    (request.getRequestURI.substring(request.getContextPath.length + 1) match {
      case path if path.startsWith(apiv3repos) => path.substring(apiv3repos.length)
      case path if path.startsWith(apiv3Orgs)  => path.substring(apiv3Orgs.length)
      case path                                => path
    }).split("/")

  def parseBaseUrl(req: HttpServletRequest): String = {
    val url         = req.getRequestURL.toString
    val path        = req.getRequestURI
    val contextPath = req.getContextPath
    val len         = url.length - path.length + contextPath.length

    val base = url.substring(0, len).stripSuffix("/")
    Option(req.getHeader("X-Forwarded-Proto"))
      .map(_.toLowerCase())
      .filter(HttpProtocols.contains)
      .fold(base)(_ + base.dropWhile(_ != ':'))
  }
}
