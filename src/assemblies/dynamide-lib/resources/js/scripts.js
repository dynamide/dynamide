//Copyright (c) 2001, 2002 DYNAMIDE.COM
//alert('loading scripts.js');
//There is a tester for this unit: js/test.html

var scripts_js_included = true;   //see windowing.js for an example of how to test for this.

  //============= Constants ==========================

  var IDE_HEIGHT = 180;
  var WINPARAMS_IDE =           'resizable,scrollbars,left=0,top=0,width=1300,height='+IDE_HEIGHT;  //width should be 1260 or screen.width.
  var WINPARAMS_TOOLBAR =       'resizable,left=0,top=0,width=200,height=300';
  var insHt = '650';
  if ( window != null ) {
    insHt = window.screen.height-IDE_HEIGHT-100;  //minus caption height, etc.... just make 100 for now %%
  }
  var WINPARAMS_INSPECTOR =     'resizable,scrollbars,left=0,top=200,width=400,height='+insHt;
  //This should have menubars off in pro...
  var WINPARAMS_NEWPAGE =       'menubar,resizable,scrollbars,left=380,top=150,width=850,height=600';//,menubar=1';
  var WINPARAMS_UTILITYPAGE =   'resizable,scrollbars,left=10,top=150,width=700,height=600';
  var WINPARAMS_SOURCEPAGE =    'resizable,scrollbars,left=100,top=200,width=900,height=600';
  var WINPARAMS_HELPPAGE =      'resizable,scrollbars,left=400,top=180,width=700,height=600';
  var WINPARAMS_TINYPAGE =      'left=0,top=0,width=100,height=100';
  var WINPARAMS_BIGPAGE  =      'resizable,scrollbars,left=100,top=80,width=1000,height=900';
  var WINPARAMS_NORMALPAGE =    WINPARAMS_SOURCEPAGE;
  var WINPARAMS_BROWSERPAGE =   'menubar,location,resizable,scrollbars,left=100,top=100,width=950,height=700';
  //=====================================================

  var debugWindow = null;

  function logDebug(msg){
     //empty for non-debug.
     try {
        debugWindow.name;
    } catch (error) {
        debugWindow = null;
    }
    try {
      if (debugWindow == null){
         debugWindow=window.open("","debuglog","left=670,top=370,width=700,height=570,resizable=1,scrollbars,menubar")
         if (debugWindow != null){
            if ((""+debugWindow.document.body.innerHTML) == ""){
                debugWindow.document.write("<html><head><title>Dynamide Debug Log</title>"
                                    +"   <style>pre {font: 12px; font-weight: bold; }</style>"
                                    +"</head><body bgcolor='#A4BFDD'><pre>Debug Log\r\n");
            }
         }
      }
      debugWindow.document.write("<br><hr>["+randomParam()+"] "+msg+"\r\n<br/>");
      //debugWindow.focus();
    } catch (error2){
        alert("ERROR using logDebug.  Execution will continue. error: "+error2);
    }
  }

  function randomParam(){
    var dNow = new Date();
    var sNow = dNow.getTime();
    return sNow;
  }

  function setSelectedByValue(theSelect, value){
        //logDebug("setSelectedByValue("+value+") selectedIndex = "+theSelect.selectedIndex);
        for (var r = theSelect.options.length - 1; r>=0; r=r-1) {
        //logDebug("innerText: "+theSelect.options[r].innerText+" value: "+value);
            var theText = ""+theSelect.options[r].innerText;
            if ( theText == value ) {
                theSelect.selectedIndex = -1;
                theSelect.selectedIndex = r;
                //logDebug("setSelectedByValue returning, selectedIndex = "+theSelect.selectedIndex);
                return;
            }
        }
        //logDebug("setSelectedByValue: false");
        return;
  }

  //========== dump ============================================

  function dumpName(ObjName){
    var result=""

    // Convert the passed text to an actual object
    Obj=eval(ObjName)

    // Step through object and get information about all of its
    // child objects, properties, etc.
    var val = "";
    for (var eachItem in Obj){
      val = Obj[eachItem]
      if ((""+val).length>0 && val != null){
          result+=ObjName+"."+eachItem+"="+val+"\r\n"
      }
    }
    return result;
  }

  function dump( Obj ) {
    return dump(Obj, false);
  }

  function dump(Obj, showEmpties){
    var result=""
    var emptyRes=""
    var val = "";

    // Step through object and get information about all of its
    // child objects, properties, etc.
    for (var eachItem in Obj){
      val = Obj[eachItem]
      if ((""+val).length>0 && val != null){
          result+=Obj.name+"."+eachItem+"="+val+"\r\n"
      } else {
          emptyRes += Obj.name+"."+eachItem+"="+val+"\r\n"
      }
    }
    if (showEmpties){
         result += "\r\n----------------------------------\r\n"+emptyRes;
    }
    return result;
  }

//alert('scripts.js loaded');
  //===================================================