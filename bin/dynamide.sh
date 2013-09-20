#!/bin/sh

. $DYNAMIDE_HOME/bin/setjavaClasspathDynamide.sh

echo CLASSPATH: $CLASSPATH
echo 
java -cp "$CLASSPATH" -DDYNAMIDE_HOME=$DYNAMIDE_HOME com.dynamide.Session $*
