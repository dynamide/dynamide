call .dynamide.local.bat
pushd %DYNAMIDE_HOME%
call %ANT_HOME%\bin\ant.bat %*
popd