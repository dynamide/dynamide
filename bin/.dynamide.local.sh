#!/bin/sh
#
# WARNING:
#
# Do NOT modify this file (.dynamide.local.sh) since it is generated
#   from ${DYNAMIDE_HOME}/bin/.dynamide.local.sh.template
#   by ${DYNAMIDE_HOME}build.xml
# Instead, modify dynamide.local.properties, and re-run the Ant task "install-bin", e.g.
#
#   cd $DYNAMIDE_HOME
#   $ANT_HOME/bin/ant install-bin
#
# You can run this file with -quiet to supress warning messages,
#   or with -verbose to see more info.

export ANT_HOME=${myenv.ANT_HOME}
export RESIN_HOME=${RESIN_HOME}

export DYNAMIDE_HOME=/Users/laramie/dynamideco/dynamide
export DYNAMIDE_RESOURCE_ROOT=/Users/laramie/dynamideco/dynamide/build/ROOT
export DYNAMIDE_LOGS=/Users/laramie/tmp/dynamide-logs
export DYNAMIDE_RESIN_CONF=/Users/laramie/dynamideco/dynamide/build/ROOT/conf/resin.dynamide.conf

export JAVA_HOME=/Library/Java/Home

export perl=${PERL}

case $1 in
  -h|-help|--help)
      echo Usage: .dynamide.local.sh \[ -h \| -help \| --help \| -quiet \| -verbose \]
      break;;
  -quiet)
      break;;
  *)
      if [ -z "$ANT_HOME" ] ; then
        echo WARNING: ANT_HOME not set
      fi

      if [ ! -d "$ANT_HOME" ] ; then
        echo WARNING: ANT_HOME \"$ANT_HOME\" does not exist
      fi

      if [ -z "$RESIN_HOME" ] ; then
        echo WARNING: RESIN_HOME not set
      fi

      if [ ! -d "$RESIN_HOME" ] ; then
        echo WARNING: RESIN_HOME \"$RESIN_HOME\" does not exist
      fi

      if [ -z "$DYNAMIDE_HOME" ] ; then
        echo WARNING: DYNAMIDE_HOME not set
      fi

      if [ ! -d "$DYNAMIDE_HOME" ] ; then
        echo WARNING: DYNAMIDE_HOME \"$DYNAMIDE_HOME\" does not exist
      fi

      if [ -z "$DYNAMIDE_RESIN_CONF" ] ; then
        echo WARNING: DYNAMIDE_RESIN_CONF not set
      fi

      if [ ! -f "$DYNAMIDE_RESIN_CONF" ] ; then
        echo WARNING: DYNAMIDE_RESIN_CONF \"$DYNAMIDE_RESIN_CONF\" does not exist
      fi

      if [ -z "$JAVA_HOME" ] ; then
        echo WARNING: JAVA_HOME not set
      fi

      if [ ! -d "$JAVA_HOME" ] ; then
        echo WARNING: JAVA_HOME \"$JAVA_HOME\" does not exist
      fi

      if [ -z "$perl" ] ; then
        echo WARNING: perl location not set
      fi

      if [ ! -f "$perl" ] ; then
        echo WARNING: perl \"$perl\" does not exist
      fi
      break;;
esac

if [ "$1" = "-verbose" ] ; then
    echo
    echo ANT_HOME: $ANT_HOME
    echo DYNAMIDE_HOME: $DYNAMIDE_HOME
    echo DYNAMIDE_RESIN_CONF: $DYNAMIDE_RESIN_CONF
    echo JAVE_HOME: $JAVE_HOME
    echo RESIN_HOME: $RESIN_HOME
    echo perl: $perl
fi