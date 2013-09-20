@echo YOU MUST MODIFY THIS FILE, THEN RUN IT
@echo off

rem set RUNDIR to the base of your installation.  All files go under that directory.
rem   Set the value of RUNDIR below in the "set" statement to your desired location,
rem     which would be the full path name of the current directory if you just want it here.
rem     The following example would work as-is if you had unzipped the distribution in C:\temp\rundir
rem     and were in that directory.

rem  Example:
rem    set  RUNDIR=C:\temp\rundir

set RUNDIR=C:\temp\zanzibar

if "%RUNDIR%" == "" goto INSTALL

ant -buildfile %RUNDIR%\com\dynamide\build.xml  -DRUNDIR=%RUNDIR% install
goto END

:INSTALL

start notepad .\install.bat

@echo on
 :END