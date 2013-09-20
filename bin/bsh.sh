#!/bin/sh

. $DYNAMIDE_HOME/bin/setjavaClasspathDynamide.sh

java -DDYNAMIDE_HOME=$DYNAMIDE_HOME bsh.Interpreter $*