#!/bin/sh

echo ===============================================
echo "          JUnit tests for Dynamide  "
echo ===============================================


. $DYNAMIDE_HOME/dynamide.local.properties

#Called setjavaClasspathDynamide.sh for the env side effects,
# now override the classpath, since junit uses XSL beans of a different version.
export CLASSPATH=$DYNAMIDE_HOME/lib/junit.jar

echo DYNAMIDE_HOME=$DYNAMIDE_HOME
echo
 
pushd $DYNAMIDE_HOME

ant -DDYNAMIDE_HOME=$DYNAMIDE_HOME test-report

popd
