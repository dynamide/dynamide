@echo off
if %1a==a goto doUsage
    @call setjavaClasspathDynamide.bat -quiet
    if %DYNAMIDE_CHECK% == 1 (
    goto END
    )
    @rem echo calling: java -Xmx512M -DDYNAMIDE_HOME="%DYNAMIDE_HOME%" %*
    java -Xmx512M -DDYNAMIDE_HOME="%DYNAMIDE_HOME%" %*
    rem java -DDYNAMIDE_RESOURCE_ROOT="%DYNAMIDE_RESOURCE_ROOT%" %*
@goto END

:doUsage
  @echo Usage:
  @echo     dynamide ^<java-classname^> ^<program-args^>
:END