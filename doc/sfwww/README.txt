This directory is for sync'ing with sourceforge's project web.

 cd /Users/laramie/src/dynamide/doc/sfwww/
 
 sftp laramiec@web.sourceforge.net
 cd  /home/project-web/dynamide/htdocs
 put index.html

example: 

$ sftp laramiec@web.sourceforge.net
Connecting to web.sourceforge.net...
laramiec@web.sourceforge.net's password: 
sftp> cd /home/project-web/dynamide/htdocs                          
sftp> dir
index.html  
sftp> put index.html
Uploading index.html to /home/project-web/dynamide/htdocs/index.html
index.html
sftp> 

