val ScalatraVersion = "2.7.0"

organization := "rz"

name := "rzgitserver"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.1"

resolvers += Classpaths.typesafeReleases

val JgitVersion = "5.7.0.202003110725-r"

libraryDependencies ++= Seq(
  "org.eclipse.jgit"        % "org.eclipse.jgit.http.server" % JgitVersion,
  "org.eclipse.jgit"        % "org.eclipse.jgit.archive"     % JgitVersion,
  "org.apache.sshd"         % "apache-sshd"                  % "2.1.0" exclude ("org.slf4j", "slf4j-jdk14") exclude ("org.apache.sshd", "sshd-mina") exclude ("org.apache.sshd", "sshd-netty"),
  "org.scalatra"            %% "scalatra"                    % ScalatraVersion,
  "org.scalatra"            %% "scalatra-scalatest"          % ScalatraVersion % "test",
  "org.eclipse.jetty"       % "jetty-webapp"                 % "9.4.19.v20190610" % "container",
  "javax.servlet"           % "javax.servlet-api"            % "3.1.0" % "provided",
  "org.mindrot"             % "jbcrypt"                      % "0.3m",
  "com.typesafe"            % "config"                       % "1.2.1",
  "com.zaxxer"              % "HikariCP"                     % "3.4.3",
  "org.postgresql"          % "postgresql"                   % "42.2.6",
  "org.playframework.anorm" %% "anorm"                       % "2.6.5",
  "ch.qos.logback"          % "logback-classic"              % "1.2.3" % "runtime",
  "javax.inject" % "javax.inject" % "1"
)
enablePlugins(ScalatraPlugin)

scalacOptions := Seq("-deprecation", "-language:postfixOps", "-opt:l:method")
