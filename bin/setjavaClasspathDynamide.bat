@rem @echo ============ setjavaClasspathDynamide.bat

@echo off
set DYNAMIDE_CHECK=0
@if "%DYNAMIDE_HOME%" == "" (
  @echo ERROR: environment variable DYNAMIDE_HOME must be set.
  set DYNAMIDE_CHECK=1
  goto END
)

rem if exists...
rem call %HOME%\.dynamide.local.bat
rem else
rem     call %HOME%\.dynamide.local.bat
call %DYNAMIDE_HOME%\bin\.dynamide.local.bat

@if "%JAVA_HOME%" == "" (
  @echo ERROR: environment variable JAVA_HOME must be set.
  set DYNAMIDE_CHECK=1
  goto END
)
@if "%ANT_HOME%" == "" (
  @echo WARNING: environment variable ANT_HOME is not set.
)

if "%DYNAMIDE_RESOURCE_ROOT%" == "" (
    set DYNAMIDE_RESOURCE_ROOT=%DYNAMIDE_HOME%\build\ROOT
)
set cp=%DYNAMIDE_HOME%\build\classes
for %%i in (%DYNAMIDE_HOME%\lib\*.jar) do call %DYNAMIDE_HOME%\bin\addcp.bat %%i
rem set cp=%cp%;C:\dynamide\build\ROOT2\homes\dynamide\assemblies\com-dynamide-apps-1\apps\swarm\resources\classes
rem set cp=%cp%;C:\bin\jdkee14\lib\j2ee.jar
set cp=%cp%;%DYNAMIDE_RESOURCE_ROOT%\classes
set cp=%cp%;%DYNAMIDE_RESOURCE_ROOT%\homes\dynamide\assemblies\com-dynamide-apps-1\apps\swarm\resources\classes
@rem set cp=%cp%;.
set CLASSPATH=%cp%
if "%1" == "-quiet" (
 rem echo mojo
) else (
    echo %1 :: %cp%
    rem this was on my other machine: call \bin\showclasspath.bat
)
:END