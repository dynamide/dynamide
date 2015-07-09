build:
    cd ~/src/dynamide
    ant
    
deploy: 
    cp $DYNAMIDE_HOME/build/lib/dynamide.jar $DYNAMIDE_HOME/build/tomcat/webapps/ROOT/WEB-INF/lib/dynamide.jar
    
restart: 
    cd ~/bin/tomcat7.dm/bin
    ./catalina.sh  run
    
debug: 
    cd ~/bin/tomcat7.dm/bin
    ./catalina.sh jpda run    #defaults to port 8000, so use that setting in a "remote" in IntelliJ.
        
Copying source from build/ to src/ :
    find the changed files: 
        cd ~/src/dynamide
        find . -mnewer .   ##compares this to the current dir, or use any file that has the date you want to compare to.
    then copy the files to src:    
        cp ./build/resource_root/homes/dynamide/assemblies/com-dynamide-test-suite-1/apps/test-suite/upload/page1.xml ./src/assemblies/dynamide-test-suite/apps/test-suite/upload/page1.xml
        cp ./build/resource_root/homes/dynamide/assemblies/com-dynamide-test-suite-1/apps/test-suite/upload/application.xml ./src/assemblies/dynamide-test-suite/apps/test-suite/upload/
  
todo:

    1)    INPUT widget (com.dynamide.edit) has textWidth and textSize, why not just use size and maxlength?
    
    2)    Add Show All Sessions to sessionDetail pages in admin. 
    
    3)    Tried to publish /dynamide/doco, but it is not working...
    
             ~/src/dynamide/bin (master)
            $ export DYNAMIDE_RESOURCE_ROOT=/Users/vcrocla/src/dynamide/build/resource_root/
            
             ~/src/dynamide/bin (master)
            $ ./publish.sh -o ~/tmp/ /dynamide/doco
            
    4) 
        Log headers is not working: 
        
                // %% this isn't working: !! LOG_HANDLER_PROC_MISSING_EVENTS = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY, org.apache.log4j.Priority.DEBUG);
                //System.out.println("!!!!!!!!!!"+Constants.LOG_HANDLER_PROC_MISSING_EVENTS_CATEGORY+" "
                //                   +" LOG_HANDLER_PROC_MISSING_EVENTS: "+LOG_HANDLER_PROC_MISSING_EVENTS
                //                   +" log: "+Log.getInstance());
                //
                LOG_HANDLER_PROC_REQUEST_HEADERS = Log.getInstance().isEnabledFor(Constants.LOG_HANDLER_PROC_REQUEST_HEADERS, org.apache.log4j.Priority.DEBUG);

        This file should be the one to modify, and the stdout startup message confirms this.
        /Users/vcrocla/src/dynamide/build/resource_root/conf/log.conf

