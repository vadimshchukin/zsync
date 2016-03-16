# zsync
Overview
-----------
z/OS data set uploading utility. Efficently uploads/synchronizes files to a z/OS system using only standard FTP connection. The JAR file includes all dependencies.

Features
-----------
  - Uploads only files that have beeen added/changed on the local side.
  - Removes files that have been deleted on the local side.

Command-line options
-----------
```text
 -h,--help                print this help and exit
 -s,--hostname <arg>      FTP hostname
 -u,--username <arg>      FTP username
 -p,--password <arg>      FTP password
 -l,--local-root <arg>    local root
 -r,--remote-root <arg>   remote root
 -i,--list                list
 -v,--verbose             verbose
 -o,--upload              upload
```

Examples
-----------
Uploads added/changed files in the "local root directory" to the "remote data set name prefix" sandbox:
```sh
$ java -jar zsync.jar -s"server address" -u"FTP username" -p"FTP password" -l"local root directory" -r"remote data set name prefix" -o
```

Dependencies
-----------

zsync depends on the following libraries:

* [Apache Commons CLI].
* [Apache Commons Net].

[Apache Commons CLI]:https://commons.apache.org/cli/
[Apache Commons Net]:https://commons.apache.org/net/
