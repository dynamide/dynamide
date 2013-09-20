call c:\dynamide\bin\setDynamideLocal.bat
pushd %TOMCAT_HOME%
rem set JPDA_ADDRESS=8000 
rem set JPDA_TRANSPORT=dt_socket
set JPDA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n 
echo USING DEBUGGING: catalina.bat jpda start
call bin\catalina.bat jpda start
popd