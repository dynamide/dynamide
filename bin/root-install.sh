#!/bin/sh
#  Run this script as root to initialize the directories.
#  User "dynamide" and group "dynamide" should exist prior to running this script.

if [ -z "$DYNAMIDE_HOME" ] ; then
  echo ERROR: DYNAMIDE_HOME environment variable not set.
else
  chgrp -R dynamide $DYNAMIDE_HOME
  chown -R dynamide $DYNAMIDE_HOME
  chmod 754 $DYNAMIDE_HOME/bin/* $DYNAMIDE_HOME/bin/.*

  echo Directories set up.
  echo
  echo Now su to the user "dynamide" and run ./install.sh
  echo   to complete the installation.
  echo
  echo Make sure that the Resin log directories are writable by the "dynamide" user.
fi