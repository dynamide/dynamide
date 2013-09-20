//Copyright (c) 2001, 2002 DYNAMIDE.COM

// **************************************************************************
// *******   IMPORTANT: Depends on scripts.js, so include that first.  ******
// **************************************************************************
var scripts_js_included;
if (!scripts_js_included) {
    alert('ERROR: windowing.js depends on scripts.js, which was not included');
}

//========= Generic View Management============================

var gLogViewActions = false; //turn this on to see the logDebug messages for this section

var gCascadeAmount = 0;

var gAnonymousLayoutViewCount = 0;

var gViews = new Object();

function resetCascade(){
    gCascadeAmount = 0;
}

function popup(url, newWinName, params){
  popupNewView(gViews, url, newWinName, newWinName, params, false, false, false);
}

// You can call this with url == "" to get a blank window.
// cascade and noFocus are optional parameters
//     noFocus means that after creating the window, newWindow.focus() will NOT be called automatically
//     cascade means that the cascade algorythm will be used to layout the window placement on the screen.
//      %% TODO: set up the defaults for this: use moveto 0,0 before doing it, and look at screen.size, etc.
function popupNewView(viewArray, url, newWinName, theCaption, params, cascade, noFocus, doClose){
    removeView(viewArray, newWinName, doClose);
    var theRecord = createView(url, newWinName, params, theCaption);
    if (theRecord == null){
        if (gLogViewActions) logDebug("called createView: "+newWinName+" theRecord is null!!!!");
        return "";
    } else {
        addView(viewArray, theRecord.name, theRecord);
        if (noFocus != null && noFocus) {
            //dont show.
        } else {
            showViewWin(theRecord.window, cascade);
        }
        return theRecord.name;
    }

}

//This function is identical to popupNewView, except that it does not return a value.
// This makes it useful for inline javascript urls, so that they don't go to a blank window.
// The rule is that if an href points to a function, if that function has NO return value,
// Then the href doesn't shift pages.
function popupNewViewNoReturn(viewArray, url, newWinName, params, cascade, noFocus){
    popupNewView(viewArray, url, newWinName, newWinName, params, cascade, noFocus);
}
//Call this to close a window and remove it (mark as invalid) from the list.
// You need to have the window name.  You can get that as the return value
// from popupNewView when you created the window.
function removeView(viewArray, aname, doClose){
    if (viewArray == null){
        alert("removeView: viewArray is null");
        return;
    }
    for (var item in viewArray){
        if (item == null || viewArray[item] == null){
            alert("removeView: item or viewArray[item] is null");
            return;
        }
        var rec = viewArray[item];
        if (rec.name == aname) {
            if (gLogViewActions) logDebug("removeView: [1.0]"+rec.name+"  "+rec.url);
            rec.valid = false;
            if (doClose!=null && doClose){
              if (gLogViewActions) logDebug("removeView: doClose: "+doClose);
              try {
                rec.window.close();
              } catch (error) {
                if (gLogViewActions) logDebug("error calling rec.window.close(): "+error);
              }
            }
            rec.window = null;
        }
    }
}

//Close all windows in the list, and remove them (mark them as invalid) on the list.
function closeAllViews(viewArray){
    for (var item in viewArray){
        if (item == null || viewArray[item] == null){
            alert("removeView: item or viewArray[item] is null");
            return;
        }
        var rec = viewArray[item];
        if (gLogViewActions) logDebug("closeAllViewObjects: "+rec.name+"  "+rec.url);
        rec.valid = false;
        try {
            rec.window.close();
        } catch (error) {
            if (gLogViewActions) logDebug("error calling rec.window.close(): "+error);
        }
        rec.window = null;
    }
}

// Call focus() on all windows in list.
function showAllViews(viewArray){
    if (gLogViewActions) logDebug("===== showViewObjects =====");
    for (var item in viewArray){
        if (gLogViewActions) logDebug(" ----- "+viewArray[item].name+"  "+viewArray[item].valid+"  "+viewArray[item].url);
        var win = viewArray[item].window;
        if (viewArray[item].valid) {
            showViewWin(win);
        }
    }
}

// Call focus() on named window in list.
function showView(viewArray, name){
    if (viewArray[name] != null){
    win = viewArray[name].window;
    showViewWin(win, false);
    } else {
    alert("Couldn't find ["+name+"] in viewArray");
    }
}

function getView(viewArray, name){
    var rec = viewArray[name];
    if (rec != null){
        if ( rec.valid ) {
            return rec.window;
        }
    } else {
        return null;
    }
}

// return is the window object.  when you are done, call "if (tempwin) tempwin.close();"
function messageBox(title, message){
    var w = 400;
    var h = 130;
    var x = (screen.width / 2) - (w/2);
    var y = (screen.height /2) - (h/2);
    var tempwin = window.open("", "temp"+randomParam(), "left="+x+",top="+y+",width="+w+",height="+h);
    tempwin.document.open();
    tempwin.document.write("<html><head><title>"+title+"</title></head><body bgcolor='white'><center>"+message+"<br /><br /><a id='alink' href='javascript:window.close();'><b>OK</b></a></center><script language='javascript'>alink.focus();</script></body></html>");
    tempwin.document.close();
    return tempwin;
}

//========= Internal Functions ==============================

// Internal function.
function generatePageName(){
        return "page"+(++gAnonymousLayoutViewCount);
}

// Internal function.
function createView(url, name, params, theCaption){
    var pagename = name;
    if (name == null){
        pagename = generatePageName();
    }
    var thewin = window.open(url,
                        pagename,
                        params);
    thewin.name = pagename;
    var rec = new Object();
    rec.name = ""+pagename;
    rec.caption = theCaption;
    rec.valid = true;
    rec.url = url;
    rec.window = thewin;
    if (gLogViewActions) logDebug("createView: "+rec.name+"  "+rec.valid+"  "+rec.url);
    return rec;
}

// Internal function.
function addView(viewArray, name, rec){
    viewArray[name] = rec;
}

// Internal function.
function showViewWin(win, cascade){
    if (win != null){
        try {
            if (cascade != null && cascade){
                win.moveBy(gCascadeAmount, gCascadeAmount);
                gCascadeAmount += 20;
                if (gCascadeAmount > 300){
                    gCascadeAmount = 0;
                }
            } else {
                win.moveBy(1, 0);
                win.focus();
                win.moveBy(-1, 0);
            if (gLogViewActions) logDebug(" ---------- showViewWin: "+win);

            }
            win.focus();
        } catch (error) {
            //window was closed by user.  just be silent.
        }
    } else {
        alert("Can't show window in showViewWin: window is null");
    }
}

//========= Functions for testing ==============================

function listViewObjects(viewArray, showAll){
    if (gLogViewActions) logDebug("===== listViewObjects =====");
    for (var item in viewArray){
        if ((showAll != null && showAll) || viewArray[item].valid){
            if (gLogViewActions) logDebug(" ---------- "+viewArray[item].name+"  "+viewArray[item].valid+"  "+viewArray[item].url);
        }
    }
}

function testRemoveViews(){
    if (gLogViewActions) logDebug("====== testremoveViews ======");
    var viewArray = new Object();
    popupNewView(viewArray, null, "c", "c", WINPARAMS_TINYPAGE, true);
    removeView(viewArray, "c");
}

function testViews(viewArray){
    if (gLogViewActions) logDebug("====== testViewObjects ======");
    popupNewView(viewArray, "/dynamide/demo", "demo", "demo", WINPARAMS_NEWPAGE, true);
    popupNewView(viewArray, "/test.html", "mytest", "mytest", WINPARAMS_NEWPAGE, true);
    popupNewView(viewArray, "/dynamide/test", null, null, WINPARAMS_NEWPAGE, true);
    popupNewView(viewArray, "", "_blank", "_blank", WINPARAMS_NEWPAGE, true);
}

//======================================================================