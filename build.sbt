val ScalatraVersion = "2.7.0"

organization := "rz"

name := "rzgitserver"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.1"

resolvers += Classpaths.typesafeReleases

val JgitVersion = "5.7.0.202003110725-r"

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % JgitVersion,
  "org.eclipse.jgit" % "org.eclipse.jgit.archive" % JgitVersion,
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.19.v20190610" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.typesafe" % "config" % "1.2.1"
)

enablePlugins(ScalatraPlugin)

scalacOptions := Seq("-deprecation", "-language:postfixOps", "-opt:l:method")
