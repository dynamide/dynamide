#!/bin/sh

if [ -z "$DYNAMIDE_HOME" ] ; then
  echo ERROR: DYNAMIDE_HOME not set
  exit 1
fi

if [ -z "$1" ]; then
  echo No action.  Should be one of: start, stop, restart
  exit 1
fi

action=$1


## Oddly, java -server is slower than without it.
#This script sets up the classpath, and all the environtment variables for Dynamide in general:
. $DYNAMIDE_HOME/bin/setjavaClasspathDynamide.sh

echo
echo CLASSPATH: $CLASSPATH
echo
$RESIN_HOME/bin/httpd.sh -J-Xmx512m -verbose \
      -J-DDYNAMIDE_HOME=$DYNAMIDE_HOME \
      -conf $DYNAMIDE_RESIN_CONF \
      $action \
      -pid $DYNAMIDE_LOGS/resin.httpd.pid \
      -stdout $DYNAMIDE_LOGS/resin.out \
      -stderr $DYNAMIDE_LOGS/resin.out \
      >> $DYNAMIDE_LOGS/resin.out 2>&1  &

echo see stdout, log:       $DYNAMIDE_LOGS/resin.out
#echo see stderr from resin: $DYNAMIDE_LOGS/resin.err



