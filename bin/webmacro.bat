@echo off
pushd .
@rem call dynamide.bat com.dynamide.util.WebMacroTools -emulatePage %*
@rem we need to pass in a log.conf file, since the one in the jar is not being picked up.
call dynamide.bat com.dynamide.util.WebMacroTools  -logconf %DYNAMIDE_RESOURCE_ROOT%/conf/log.conf %*
popd