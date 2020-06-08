package models

import javax.servlet.http.HttpServletRequest

case class BaseUrl(url: String)

case object HttpProtocols {
  val list                         = Vector("http", "https")
  def contains(s: String): Boolean = list.contains(s)
}

object BaseUrl {
  def apply(req: HttpServletRequest): BaseUrl = {
    val url         = req.getRequestURL.toString
    val path        = req.getRequestURI
    val contextPath = req.getContextPath
    val len         = url.length - path.length + contextPath.length

    val base = url.substring(0, len).stripSuffix("/")
    val baseUrl = Option(req.getHeader("X-Forwarded-Proto"))
      .map(_.toLowerCase())
      .filter(HttpProtocols.contains)
      .fold(base)(_ + base.dropWhile(_ != ':'))
    new BaseUrl(baseUrl)
  }
}
