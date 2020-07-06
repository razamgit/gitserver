# Razam Git Server  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/razamgit/gitserver/blob/master/LICENSE)  [![Build Status](https://travis-ci.org/razamgit/gitserver.svg?branch=master)](https://travis-ci.org/razamgit/gitserver)

Git server implemented in Scala using Scalatra, JGit and Anorm with authentication against [Razam](https://github.com/razamgit/razam) database.

## Debug

To recompile and reload your application automatically, run the following:

```sh
$ sbt
> ~;jetty:stop;jetty:start
```

```

git clone ssh://admin@localhost:2200/admin/test.git # ssh
```

## Build and Deploy

Download [Jetty server](https://www.eclipse.org/jetty/download.html), unpack and set $JETTY_HOME.

```
sbt package
cd target/scala-2.13 && cp *.war $JETTY_HOME/webapps
java -jar start.jar
```

## Copyright

Copyright 2020 Eugene Bosiakov (@bosiakov).

Package *git* contains parts of code inherited from GitBucket project.
