# Git Maven Tools

A small utility library that aids releasing Maven projects. It follows semantic versioning principles and auto-creates development branches for snapshots so that stable releases are only built from protected master branch. Built with Scala and JGit.

## Example Run
```
$ java -jar git-mvn-tool.jar -release=minor -dir=/path/to/foobar-project

===========================================================
| release summary of 'com.acme/foobar-project'            |
===========================================================
| MAVEN                                                   |
-----------------------------------------------------------
| current version                      | 1.0.45-SNAPSHOT  |
| released (minor) version             | 1.0.0            |
| next dev version                     | 1.1.0-SNAPSHOT   |
-----------------------------------------------------------
| GIT                                                     |
-----------------------------------------------------------
| previous dev branch                  | v0.1.x           |
| next dev branch                      | v1.1.x           |
-----------------------------------------------------------

continue and apply changes? [yes]/no > _
```
