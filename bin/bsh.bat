@echo off
@call %DYNAMIDE_HOME%\bin\setjavaClasspathDynamide.bat -quiet
@rem @call setjavaClasspathDynamide.bat -quiet
if %DYNAMIDE_CHECK% == 1 (
    goto END
)
set saveUserPath=%path%

rem set path=C:\bin\jdk13\bin;%path%
rem set JAVA_HOME=C:\bin\jdk13

rem set path=C:\bin\jdk141\bin;%path%
rem set JAVA_HOME=C:\bin\jdk141

path=%JAVA_HOME%\bin
@echo off
rem @echo on
rem path
java -Xmx512M  -DDYNAMIDE_HOME="%DYNAMIDE_HOME%" bsh.Interpreter %*
@set path=%saveUserPath%
:END
