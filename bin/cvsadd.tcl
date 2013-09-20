## To use this unit, source it from tclsh, then run one of the
## entry functions, e.g. lowerCaseAllFiles, or dosifyRecursive.

set CVS_BINARY ""


puts "To run these procedures, be sure to use full paths only"
puts " not relative paths like ./foo"

proc FindFile { startDir namePat proc test} {
    puts "in FindFile"
    set pwd [pwd]
    if [catch {

        set len [string length $startDir]
        set st [expr $len - 3]
        set endo [string range $startDir $st end]
        if [string match $endo "CVS"] {
            puts "skipping CVS: $startDir"
            return
        }

        if [catch {cd $startDir} err] {
		puts stderr $err
		return
	}
        foreach match [glob -nocomplain -- $namePat] {
                $proc $startDir $match [file join $startDir $match] $test
	}
	foreach file [glob -nocomplain *] {
		if [file isdirectory $file] {
                    FindFile $startDir/$file $namePat $proc  $test
		}
	}
    } errstr ] {
        puts $errstr
    }
    cd $pwd
}


proc cvsAddInner {startDir m n test} {
        global CVS_BINARY
        set len [string length $startDir]
        set st [expr $len - 3]
        set endo [string range $startDir $st end]
        if [string match $m "CVS"] {
            puts "Skipping CVS: $startDir"
            return
        } else {
           #puts "endo: $endo st: $st m: $m"
           puts "looking at $n"
           if [ catch {
              if {$test} {
                set res "\[test mode\] $CVS_BINARY add $n"
              } else {
                set res [exec $CVS_BINARY add $n]
              }
              puts $res
           } errstr] {
              puts $errstr
           }
           #puts "Not skipping: $startDir"
        }
}

proc cvsadd {cvsbinary startdir pattern test} {
    global CVS_BINARY
    set CVS_BINARY $cvsbinary
    puts "Adding files"
    FindFile $startdir $pattern cvsAddInner $test
}
