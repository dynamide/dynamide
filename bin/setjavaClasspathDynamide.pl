#!/usr/bin/perl
#
# Set the classpath
#
#DON'T PUT ANY PRINT STATEMENTS IN HERE, THEY END UP ON THE CLASSPATH.

$DYNAMIDE_HOME=$ENV{"DYNAMIDE_HOME"};
$DYNAMIDE_RESOURCE_ROOT=$ENV{"DYNAMIDE_RESOURCE_ROOT"};
$RESIN_HOME=$ENV{"RESIN_HOME"};
$ANT_HOME=$ENV{"ANT_HOME"};

## DON'T PULL THIS IN:  $CLASSPATH=$ENV{"CLASSPATH"};

$CLASSPATH="$DYNAMIDE_HOME/build/classes";

# dynamide.${dynamide.version}.jar will be included automatically, since we put it in lib
opendir(DMLIB, "$DYNAMIDE_HOME/lib");
while ($file = readdir(DMLIB)) {
    if ($file !~ /\.jar$/ && $file !~ /\.zip$/) {
    }
    elsif ($file =~ /jsdk.*\.jar/) {
    }
    elsif ($file =~ /CVS/) {
    }
    elsif ($file eq "jdk12.jar") {
    }
    else {
        $CLASSPATH="$CLASSPATH:$DYNAMIDE_HOME/lib/$file";
    }
}
closedir(DMLIB);

if (-d "$DYNAMIDE_RESOURCE_ROOT") {
    $CLASSPATH="$CLASSPATH:$DYNAMIDE_RESOURCE_ROOT/classes";
}

if (-d "$RESIN_HOME") {
    $CLASSPATH="$CLASSPATH:$RESIN_HOME/lib/jsdk23.jar";
}

print($CLASSPATH);