c:
pushd C:\dynamide
if a%1 == a (
   set whichtest=test-report
) else (
   set whichtest=%1
)
set CLASSPATH=C:\dynamide\lib\junit.jar
call ant -DDYNAMIDE_HOME="%DYNAMIDE_HOME%" %whichtest%
if %whichtest% == test-report (
    start C:\dynamide\build\test-results\html\index.html
)
popd