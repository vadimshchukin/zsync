# zsync
Overview
-----------
z/OS data set uploading utility. Efficently uploads/synchronizes files to a z/OS system using only standard FTP connection. The JAR file includes all dependencies.

Features
-----------
  - Uploads only files that have been added/changed on the local side.
  - Removes files that have been deleted on the local side.
  - Creates data sets with specified allocation parameters if neccesary.

Command-line options
-----------
```text
 -h,--help                     print this help and exit
 -s,--hostname <arg>           FTP hostname
 -u,--username <arg>           FTP username
 -p,--password <arg>           FTP password
 -l,--local-root <arg>         local root
 -r,--remote-root <arg>        remote root
 -e,--exclude-path <arg>       exclude path from the synchronization
 -d,--datasets-options <arg>   data sets allocation parameters
 -x,--index-file <arg>         index file
 -v,--verbose                  verbose
 -i,--update-index             update index
 -o,--upload                   upload
```

Examples
-----------
Uploads added/changed files in the "local root directory" to the "remote data set name prefix" sandbox:
```sh
$ java -jar zsync.jar -s"server address" -u"FTP username" -p"FTP password" -l"local root directory" -r"remote data set name prefix" -o
```

The following example shows the usage of the automatic data set allocation parameters specification. It would allocate data sets with the specified parameters if necessary:
```sh
$ java -jar zsync.jar -s"server address" -u"FTP username" -p"FTP password" -l"local root directory" -r"remote data set name prefix" -d"allocation parameters file" -vo
```
The content of the "allocation parameters file":
```text
ASM PDSTYPE=PDSE RECFM=FB LRECL=80  BLKSIZE=32720 DIRECTORY=100 SPACETYPE=TRACK PRIMARY=20 SECONDARY=100
CPP PDSTYPE=PDSE RECFM=VB LRECL=255 BLKSIZE=32720 DIRECTORY=100 SPACETYPE=TRACK PRIMARY=20 SECONDARY=100
```

Dependencies
-----------

zsync depends on the following libraries:

* [Apache Commons CLI].
* [Apache Commons Net].

[Apache Commons CLI]:https://commons.apache.org/cli/
[Apache Commons Net]:https://commons.apache.org/net/
