@echo off
set RESOURCE_ROOT=C:\temp\dynamide-sandboxes\assembly-test
rem set RESOURCE_ROOT=C:\temp\dynamide-sandboxes\RESOURCE_ROOT
if "%1" == "ResourceManager" (
    if "%2" == "-dumpContext" (
        java  com.dynamide.resource.ResourceManager -root %RESOURCE_ROOT% -dumpContext
    ) else if "%2" == "-testImports" (
        java  com.dynamide.resource.ResourceManager -root %RESOURCE_ROOT% -testImports
    ) else if "%2" == "-viewRoot" (
        java  com.dynamide.resource.ResourceManager -root %RESOURCE_ROOT% -dumpRoot -html -o \temp\resourcemanager-dump.html
        start \temp\resourcemanager-dump.html
    ) else (
        java  com.dynamide.resource.ResourceManager %*
    )
) else if "%1" == "Assembly" (
    if "%2" == "-testapps" (
        set TESTDIR=C:\temp\dynamide-sandboxes\assembly-test\homes\dynamide\assemblies\com-dynamide-apps
        java  com.dynamide.resource.Assembly -root %RESOURCE_ROOT% -dir %TESTDIR% %3 %4 %5 %6 %7 %8 %9
    ) else if "%2" == "-testdynamide" (
        set TESTDIR=C:\temp\dynamide-sandboxes\RESOURCE_ROOT\homes\dynamide\assemblies\com-dynamide-apps
        java  com.dynamide.resource.Assembly -root %RESOURCE_ROOT% -dir %TESTDIR% -account dynamide %3 %4 %5 %6 %7 %8 %9
    ) else (
        java  com.dynamide.resource.Assembly -root %RESOURCE_ROOT% %2 %3 %4 %5 %6 %7 %8 %9
    )
) else if "%1" == "Session" (
    java com.dynamide.Session %RESOURCE_ROOT% /test/import-by-interface/foo %RESOURCE_ROOT%
) else if "%1" == "--daily" (
    call %DYNAMIDE_HOME%\bin\audit.bat
    call %DYNAMIDE_HOME%\bin\junit.bat
) else (
 echo.
 echo  Usage:
 echo   test --daily
 echo   test ResourceManager -dumpContext
 echo   test ResourceManager -testImports
 echo   test ResourceManager -viewRoot  //shows in browser window
 echo   test Assembly -testapps ^<-account account^> [-list]
 echo   test Assembly -testdynamide [-list]
 echo   test Assembly ^<-dir assembly-dir^> ^< -account account^> [-list]
 echo.
 echo see also:
 echo    bsh  C:\dynamide\src\shell\tests\admin-sessionsPage.bs
 echo  Beanshell:
 echo    c:\bin\bsh.bat
 echo  Dynamide WebMacro shell:
 echo    c:\bin\webmacro.bat

)