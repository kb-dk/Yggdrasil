#!/bin/sh
ProgDir=`dirname "$0"`
. "${ProgDir}/env.sh"

if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M"
fi

cd ${assembly.home.env.name.ref}

#"${JAVA}" ${JAVA_OPTS} -D${assembly.home.env.name}="${assembly.home.env.name.ref}" -D${assembly.config.env.name}="${assembly.config.env.name.ref}" -cp "$CP" ${assembly.main.class.name} "$@"
"${JAVA}" ${JAVA_OPTS} -D${assembly.home.env.name}="${assembly.home.env.name.ref}" -D${assembly.config.env.name}="${assembly.config.env.name.ref}" -cp "$CP" dk.kb.yggdrasil.xslt.XslTransform "$@"
