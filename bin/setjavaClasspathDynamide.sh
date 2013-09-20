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

##export CLASSPATH=`$perl $DYNAMIDE_HOME/bin/setjavaClasspathDynamide.pl`
export CLASSPATH='build/lib/dynamide.jar:lib/*'

if [ "$1" = "-outputclasspath" ]; then
  echo $CLASSPATH
fi
if [ "$1" = "-verbose" ]; then
  echo
  echo $CLASSPATH
fi
