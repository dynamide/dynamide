#!/bin/sh

. $DYNAMIDE_HOME/bin/setjavaClasspathDynamide.sh

if [ "$1" = "-verbose" ]; then
    shift
    echo DYNAMIDE_HOME: "${DYNAMIDE_HOME}"
    echo DYNAMIDE_BUILD: "${DYNAMIDE_BUILD}"
    echo DYNAMIDE_RESOURCE_ROOT: "${DYNAMIDE_RESOURCE_ROOT}"
    echo DYNAMIDE_CONTEXT_CONF: "${DYNAMIDE_CONTEXT_CONF}"
    echo CLASSPATH: "$CLASSPATH"
    echo 
fi

java -cp "$CLASSPATH" -DDYNAMIDE_HOME=$DYNAMIDE_HOME com.dynamide.Session ${DYNAMIDE_RESOURCE_ROOT} $*
