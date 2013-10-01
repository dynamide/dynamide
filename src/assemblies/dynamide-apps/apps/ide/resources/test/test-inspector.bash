## source with:
## . /cygdrive/c/dynamide/build/ROOT/homes/dynamide/assemblies/com-dynamide-apps-1/apps/ide/resources/test/test-inspector.bash

curl 'http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test&action=Close'

curl 'http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test&noMainWindow=1&action=startSubsession&openProject=/mailmerge&projectPath=/mailmerge'

## this works, dumps the inspector for edit1 out to stdout:
## curl -s 'http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test&noMainWindow=1&next=inspectortop&targetPageID=testPage&widgetID=edit1' | grep 'DEBUG:\|ERROR:'

## this works, shows the inspector for linkstrip1:
##ie.bash http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test\&noMainWindow=1\&next=inspectortop\&targetPageID=testPage\&widgetID=linkstrip1

## this works, shows the designer for linkstrip1:
ie.bash http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test\&noMainWindow=1\&next=designertop\&propertyName=linkTEST\&propertyValue=\&targetOwnerID=testPage\&targetClass=Widget\&targetID=linkstrip1\&datatype=com.dynamide.datatypes.EnumeratedDatatype

## this works, shows the designer for linkstrip1:
##ie.bash http://apps.dynamide.com:7080/dynamide/ide?SESSIONID=test\&noMainWindow=1\&next=designertop\&propertyName=links\&propertyValue=key2\&targetOwnerID=testPage\&targetClass=Widget\&targetID=linkstrip1\&datatype=com.dynamide.datatypes.EnumeratedDatatype
