// This script file should be sourced by any page that is sent to the client
//  in either design or production mode.  The Dynamide IDE calls ie/scripts.js
//  for specific functions needed by the IDE.  But any page that is rendered
// from a page.xml file should pull in this one.

function randomParam(){
    var dNow = new Date();
    var sNow = dNow.getTime();
    return sNow;
}

function invalidOpener(){
    //for debugging, turn this on, otherwise be silent.
    //alert("Invalid opener (page.js)");
    return false;
}

var bNS = navigator.appName=="Netscape";
var bIE = navigator.appName=="Microsoft Internet Explorer";

// call like this, except that it doesn't work:
//    if ( isRightClick(event) ) {
//        alert('rc');
//        return false;
//    } else {
//        alert('nrc');
//    }
function isRightClick(e) {
    if (bNS && e.which > 1){
        return true;
    }else if (bIE && (event.button >1)){
        return true;
    }
    return false;
}

function bodyClicked(obj){
    debugger;
  if ( window["dmDesign_TableClicked"] == null ) {
    logDebug("<h2>dmDesign_TableClicked is null</h2>");
    widgetClicked(obj, true);
    return false;
  }
  dmDesign_TableClicked(obj);
  widgetClicked(obj, true);
}

//actually, may apply to divs or body also...
//Works in IE only.
function widgetClicked(obj, isBodyClick){
    debugger;
    if (obj.document === undefined){
        obj.document = obj.ownerDocument;
    }
    if (event != null) event.cancelBubble = true;
    if (opener == null) return invalidOpener(); //for the page designer IDE, this shouldn't apply.
    //alert(""+obj.document.all.pageID.value);
    var pageID = "";
    if ( obj.document.all.pageID != null ) {
        pageID = obj.document.all.pageID.value;
    }
    if ( pageID == null || pageID.length == 0 ) {
        var bd = obj.document.all.tags("body")[0];
        if (bd == null){
            bd = obj.document.all.tags("BODY")[0];
        }
        if ( bd != null ) {
            pageID = bd.id;
            //alert('pageID: '+pageID);
        }
    }
    if (opener.designmodeClick != null){
        opener.designmodeClick(obj, this.window, pageID, isBodyClick);
    }
}

function layoutViewFocused(pageID){
    //logDebug("page.js.layoutViewFocused("+pageID+")");
    if (opener == null) invalidOpener(); // %% for the page designer IDE, this shouldn't apply.
    opener.layoutViewFocused(pageID);
}

function getFieldValue(fieldName){
    if ( document ) {
        if ( document.all ) {
            if ( document.all[fieldName] ) {
                return document.all[fieldName].value;
            }
        }
    }
    return "";
}

function getControlFieldValue(fieldName){
    if ( document ) {
        //if bNS ...
        //else
        if ( document.all ) {
            if ( document.all[fieldName] ) {
                //handle selects, checkboxes etc...
                return document.all[fieldName].value;
            }
        }
    }
    return "";
}