#!/bin/sh
## Note.  The construct "lib/*" as a classpath element only works for java 6.



. ${DYNAMIDE_HOME}/dynamide.local.properties

export CLASSPATH="${DYNAMIDE_BUILD}/lib/"
CLASSPATH+='*'
if [ "$1" = "-outputclasspath" ]; then
  echo "$CLASSPATH"
fi
if [ "$1" = "-verbose" ]; then
  echo "======== CLASSPATH ========="
  echo "$CLASSPATH"
  echo "============================"
fi
