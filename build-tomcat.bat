@rem this must be set on each local machine.  No slick way to use env or property files yet.. :)
set TOMCAT_HOME=C:\bin\jakarta-tomcat-5.0.30



pushd c:\dynamide-private
call ant jar

echo on
copy C:\dynamide\build\lib\dynamide.jar  %TOMCAT_HOME%\shared\lib
copy C:\dynamide\build\lib\netroots.jar  %TOMCAT_HOME%\shared\lib
rem don't put jars here: C:\bin\jakarta-tomcat-5.5.9\webapps\dynamide\WEB-INF\lib
popd
