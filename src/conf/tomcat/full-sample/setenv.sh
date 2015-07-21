#This file goes in $TOMCAT_HOME/bin/
export CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=18082"

