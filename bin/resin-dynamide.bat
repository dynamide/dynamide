@call setjavaClasspathDynamide.bat -quiet
if exist "%RESIN_HOME%\bin\httpd.exe" (
    set RESIN_EXE="%RESIN_HOME%\bin\httpd.exe"
) 
if exist "%RESIN_HOME%\httpd.exe" (
    using Resin 3.0 configuration.
    set RESIN_EXE="%RESIN_HOME%\httpd.exe"
) 

if %DYNAMIDE_CHECK% == 1 (
    goto END
)
@if "%RESIN_HOME%" == "" (
  @echo ERROR: environment variable RESIN_HOME is not set.
  goto END
)
echo Starting: %RESIN_EXE% -Xmx256m -verbose -J-DDYNAMIDE_HOME=%DYNAMIDE_HOME% -conf %DYNAMIDE_RESIN_CONF%
start "Resin" %RESIN_EXE% -Xmx256m -verbose -J-DDYNAMIDE_HOME=%DYNAMIDE_HOME% -conf %DYNAMIDE_RESIN_CONF%
:END