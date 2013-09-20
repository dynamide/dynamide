@call setjavaClasspathDynamide.bat -quiet
if %DYNAMIDE_CHECK% == 1 (
    goto END
)
@if "%RESIN_HOME%" == "" (
  @echo ERROR: environment variable RESIN_HOME is not set.
  goto END
)
set path=C:\bin\jdk13\bin;%path%
set JAVA_HOME=C:\bin\jdk13
rem start "Resin"
%RESIN_HOME%\bin\httpd.exe -Xmx256m -verbose -J-DDYNAMIDE_HOME=%DYNAMIDE_HOME% -conf %DYNAMIDE_RESIN_CONF%
:END