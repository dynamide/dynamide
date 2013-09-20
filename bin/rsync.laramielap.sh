#!/bin/sh
rootdir=`cygpath -u $DYNAMIDE_HOME`
destdir=/c/temp/dynamide-sandboxes

## To set this up, you may have to run the first time with --times so that timestamps are synchronized
##options='-r -v -z --cvs-exclude --times --stats'
options='-r -v -z --cvs-exclude --times'

echo ===================================
echo " NOTE: using rsync (by timestamp)
echo ===================================
echo

rsync $options $rootdir $destdir
