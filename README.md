# Razam Git Server  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/razamgit/gitserver/blob/master/LICENSE)  [![Build Status](https://travis-ci.org/razamgit/gitserver.svg?branch=master)](https://travis-ci.org/razamgit/gitserver)

Git server implemented in Scala using Scalatra, JGit and Anorm with authentication against [Razam](https://github.com/razamgit/razam) database.

## Build & Run

```sh
$ cd rzgitserver
$ sbt
> jetty:start
```

## Copyright

Copyright 2020 Eugene Bosiakov (@bosiakov).

Package *git* contains parts of code inherited from GitBucket project.
