==========================
Overview
==========================
 For all the shell scripts to work in Dynamide, you must have
 DYNAMIDE_HOME
 set in your environment.
 Examples
 Windows:
    set DYNAMIDE_HOME=C:\dynamide
 Windows-Cygwin:
    export DYNAMIDE_HOME=C:\\dynamide
 Unix:
    export DYNAMIDE_HOME=/usr/local/dynamide


 Window-Cygwin notes:
    Do not use the .sh varieties, use the .bat varieties.
    e.g.
        bash$ export DYNAMIDE_HOME=C:\\dynamide
        bash$ cd /cygdrive/c/dynamide
        bash$ bin/setjavaClasspathDynamide.bat

    To run ant, you may have to do something like this in a little script:

        export ANT_HOME=C:\\bin\\apache-ant-1.5.3-1
        export JAVA_HOME=C:\\bin\\jdk141
        $ANT_HOME/bin/ant.bat $*

==========================
Shell script dependencies:
==========================

 Note: setjavaClasspathDynamide.sh finds .dynamide.local.sh in either
    ${DYNAMIDE_HOME}/bin/.dynamide.local.sh
 or
    ${HOME}/.dynamide.local.sh

 resin-dynamide.sh
  |
  +---- setjavaClasspathDynamide.sh
         |
         +---- .dynamide.local.sh

 bsh.sh,
 junit.sh,
 webmacro.sh
  |
  +---- dynamide.sh
         |
         +---- setjavaClasspathDynamide.sh
                |
                +---- .dynamide.local.sh