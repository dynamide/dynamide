//This may or may not work.  I went some other route, but this code does not cause js errors in the browser, and 
// is what everyone on the net says will work.
function nonWorkingCursor(){
        var theDiv = $('#SDiv').get(0);
        if (window.getSelection && document.createRange) {
            console.log("ready...");
            // IE 9+ and non-IE
            var sel = window.getSelection();
            var range = document.createRange();
            range.setStart(theDiv, 0);
            range.collapse(true);
            sel.removeAllRanges();
            sel.addRange(range);
            console.log("liftoff...");
        } 
    }
