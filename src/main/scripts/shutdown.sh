#!/bin/sh
ProgDir=`dirname "$0"`
. "${ProgDir}/env.sh"

if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-Xms256m -Xmx1024m"
fi

cd ${assembly.home.env.name.ref}

"${JAVA}" ${JAVA_OPTS} -D${assembly.home.env.name}="${assembly.home.env.name.ref}" -D${assembly.config.env.name}="${assembly.config.env.name.ref}" -D${assembly.runningmode.env.name}="${assembly.runningmode.env.name.ref}" -cp "$CP" dk.kb.yggdrasil.Shutdown "$@"
