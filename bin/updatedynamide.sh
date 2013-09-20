#!/bin/sh
echo Updating Dynamide.  To commit, use dcommit.sh
u=`uname -s|cut -c-6`
if [ "$u" = "CYGWIN" ]; then
     BASE=`cygpath -u $DYNAMIDE_HOME`
else
     BASE=$DYNAMIDE_HOME
fi
echo Using Platform-Fixed DYNAMIDE_HOME=$BASE
echo Using CVSROOT=$CVSROOT
RESOURCE_ROOT="$BASE/build/ROOT"
message=$1

echo :: update -d $BASE/bin
cvs -q update -d $BASE/bin

echo :: update -d  $BASE/doc
cvs -q update -d  $BASE/doc

echo :: update -d  $BASE/lib
cvs -q update -d  $BASE/lib

echo :: update -d  $BASE/src
cvs -q update -d  $BASE/src

echo :: update -d  $RESOURCE_ROOT/assemblies/com-dynamide-lib-1
cvs -q update -d  $RESOURCE_ROOT/assemblies/com-dynamide-lib-1

echo :: update -d  $RESOURCE_ROOT/conf
cvs -q update -d  $RESOURCE_ROOT/conf

echo :: update -d  $RESOURCE_ROOT/homes
cvs -q update -d  $RESOURCE_ROOT/homes

echo :: update -d $RESOURCE_ROOT/homes/web-apps.xml
cvs -q update -d $RESOURCE_ROOT/homes/web-apps.xml

echo :: update -d  $RESOURCE_ROOT/homes/desu-ka
cvs -q update -d  $RESOURCE_ROOT/homes/desu-ka