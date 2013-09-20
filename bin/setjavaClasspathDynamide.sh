#!/bin/sh
#Example -- you can run this to get the classpath:
#
#    /bin/sh  bin/setjavaClasspathDynamide.sh -outputclasspath

if [ -z "$DYNAMIDE_HOME" ] ; then
  echo ERROR: DYNAMIDE_HOME not set
  exit 1
fi

if [ -f "$HOME/.dynamide.local.sh"  ] ; then
  if [ "$1" = "-verbose" ]; then
      echo Using .dynamide.local.sh from HOME directory: $HOME
  fi
  . $HOME/.dynamide.local.sh
else
  if [ -f "$DYNAMIDE_HOME/bin/.dynamide.local.sh"  ] ; then
     echo Using .dynamide.local.sh from directory $DYNAMIDE_HOME/bin
     . $DYNAMIDE_HOME/bin/.dynamide.local.sh
  fi
fi


## lib/* only works for java 6
export CLASSPATH="${DYNAMIDE_HOME}/build/defaultWebapp/ROOT/WEB-INF/lib/"; CLASSPATH+='*'
echo === $CLASSPATH ===

if [ "$1" = "-outputclasspath" ]; then
  echo $CLASSPATH
fi
if [ "$1" = "-verbose" ]; then
  echo
  echo $CLASSPATH
fi
