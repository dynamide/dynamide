call c:\dynamide\bin\setDynamideLocal.bat
call tomcat-dynamide-stop.bat
pushd c:\dynamide-private\
call ant jar
popd
call tomcat-dynamide-start.bat
