# Template file for daemon-settings.sh. 
#!/bin/bash

#set -x
# Running mode of Yggdrasil, can be the following: development, test, production.
YGGDRASIL_RUNNING_MODE=
# The Java home path that should be used by Yggdrasil.
JAVA_HOME=
# The Java options to be used, the default is set below.
JAVA_OPTS="-Xms256m -Xmx1024m"
# The name of the daemon, default is yggdrasil.
DNAM="yggdrasil"
# The user under which the daemon should run, default is yggdrasil.
DUSER="yggdrasil"

