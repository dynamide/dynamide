#!/bin/sh
#. $HOME/d

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

echo :: commit -m \"$message\" $RESOURCE_ROOT/assemblies/com-dynamide-lib-1
cvs -q commit -m "$message" $RESOURCE_ROOT/assemblies/com-dynamide-lib-1

echo ::  commit -m \"$message\"  $RESOURCE_ROOT/conf
cvs -q commit -m "$message"  $RESOURCE_ROOT/conf

echo ::  commit -m \"$message\"  $RESOURCE_ROOT/homes
cvs -q commit -m "$message"  $RESOURCE_ROOT/homes

echo :: update -d  $BASE/src
cvs -q update -d  $BASE/src

echo :: commit -l -m \"$message\"  $BASE
cvs -q commit -l -m "$message"   $BASE

echo :: commit -m \"$message\"  $BASE/src
cvs -q commit -m "$message"  $BASE/src

echo :: commit -m \"$message\"  $BASE/doc
cvs -q commit -m "$message"  $BASE/doc

echo :: commit -m \"$message\"  $BASE/bin
cvs -q commit -m "$message"  $BASE/bin

echo :: commit -m \"$message\"  $BASE/lib
cvs -q commit -m "$message"  $BASE/lib

echo :: update -d $BASE/bin
cvs -q update -d $BASE/bin

echo :: update -d $BASE/lib
cvs -q update -d $BASE/lib

echo :: update -d  $RESOURCE_ROOT/assemblies/com-dynamide-lib-1
cvs -q update -d  $RESOURCE_ROOT/assemblies/com-dynamide-lib-1

echo :: update -d  $RESOURCE_ROOT/conf
cvs -q update -d  $RESOURCE_ROOT/conf

echo :: update -d  $RESOURCE_ROOT/homes
cvs -q update -d  $RESOURCE_ROOT/homes
