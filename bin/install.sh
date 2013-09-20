#!/bin/sh

if [ -z "$ANT_HOME" ] ; then
  echo WARNING: ANT_HOME not set.
  echo  Please set it in an environment variable and re-run this script.
  exit 1
fi

if [ -z "$DYNAMIDE_HOME" ] ; then
  export DYNAMIDE_HOME=`pwd`
  echo WARNING: DYNAMIDE_HOME not set -- using current directory.
  echo DYNAMIDE_HOME now: $DYNAMIDE_HOME
fi

if [ ! -f "$DYNAMIDE_HOME/dynamide.version" ] ; then
  echo ERROR: \"$DYNAMIDE_HOME\" does not seem to be a DYNAMIDE_HOME directory
  exit 1
fi

$ANT_HOME/bin/ant -DDYNAMIDE_HOME=$DYNAMIDE_HOME install-bin
