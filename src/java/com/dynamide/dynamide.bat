cls

set CLASSPATH=C:\java\com\dynamide\lib\bsh-1.1alpha5.jar;C:\java\com\dynamide\lib\jdom.jar;C:\java\com\dynamide\lib\webmacro.jar;C:\java\com\dynamide\lib\xerces.jar;C:\java\com\dynamide\lib\log4j.jar;C:\java\com\dynamide\lib\tcl.zip;C:\java\com\dynamide\lib\symbeans.jar;C:\java

set RUNDIR=C:\java

java -classpath %CLASSPATH% com.dynamide.gui.IDE %RUNDIR% %RUNDIR%\Dynamide\demo\application.xml %RUNDIR%\Dynamide\demo\page1verysimple.xml