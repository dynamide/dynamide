//dependencies: dump.js, stringUtils.js, keycodes.js

  var gSkipPreKeyHook = false;  //this is turned on when ingesting json strings from function calls, but turned off in keyboard entry mode.
  var gRunTests = true;
  
  var gLogDebug = true;
  var gLogDebugSaveToJson = false;
  
  var gQuoteMode = false;
  
  var cOBJECT = 1,
      cARRAY  = 2;
      
  //========== KEYBOARD EVENTS =================================================
    
  function trapDefault(event){
      if (event){
        if (gLogDebug) console.log("prevent default...");
        event.preventDefault();
      }
    }

  function documentKeydown(event){
        var shifted = event.shiftKey && event.shiftKey == true;
        var keycode = event.which;
        if (gLogDebug) console.log('keydown:'+keycode);
        switch ( keycode  ){
        case VK_BAK: 
            if (event){
                event.preventDefault();
            }
            if (gSlashMode){
                 backspaceTag(); 
                 $('#theInput').text(getTag());
                 timerSelectPosition($('#theInput'));
                 getAutocomplete(getTag());  
                 return;
            }
            var current = getCurrentCell();
            var cur = current.text();
            if (cur.length > 0){
                cur=cur.substring(0,cur.length-1);
                current.text(cur);
            }
            return true;
        case VK_ENTER:
            if (gSlashMode){
                gSlashMode = false;
                if (event){
                    event.preventDefault();
                }
                clearTag();
                $('#theInput').text('');
                break;
            }
            preKeyHook(VK_ENTER, event); //informational only on this call, we ignore "handled" return value.
            addRow(); 
            break;
        case VK_SPACE:
            return;  
        case VK_TAB: 
            if (shifted){
                goShiftTab();
                return false;
            } else {
                goTab();
                return false;
            }
        case VK_LEFT: 
            //var editMode = $('#ckEditMode').is(":checked");
            if (inEditMode()){
                return;
            } else {
                goShiftTab();
                return false;
            }
        case VK_RIGHT: 
            if (inEditMode()){
                return;
            } else {
                goTab();
                return false;
            }
        case VK_UP: 
            selectRowAbove();
            trapDefault(event);
            return;
        case VK_DOWN: 
            selectRowBelow();
            trapDefault(event);
            return;
        default:
        } 
  }
  
  //Use this for special keys such as arrows:
  function documentKeyup(event){
        var shifted = event.shiftKey && event.shiftKey == true;
        var keycode = event.which;
        if (gLogDebug) console.log('keyup:'+keycode+' shifted:'+shifted);
        switch ( keycode  ){
            case VK_ENTER:
                break;
            default:
        }
  }
  
  
  //Use this for typing keys
  function documentKeypress(event){
       var key = event.which;
       if (gLogDebug) console.log('keypress:'+key);
       var ch = String.fromCharCode(key);
       processKey(ch, event);
  }
  
  function processKey(ch, event){
      if (gLogDebug) console.log('processKey:'+event);
      setMessage("");
      var handled = preKeyHook(ch, event); 
      if (handled){
          return;
      }
      if (ch === CH_ENTER || ch === CH_LINEFEED){
          return;  //don't append
      }
      var current = getCurrentCell();
      if (gSlashMode){
         addToTag(ch); 
         $('#theInput').text($('#theInput').text()+ch);
         timerSelectPosition($('#theInput'));
         getAutocomplete(getTag());  
         if (event){
             event.preventDefault();
         }
         return;
      }
      // 20140930 this works, but if you pass in a string like this "/my/path" it gets hosed.
      if ((!gQuoteMode) && ch ==='/') {
          if ( ! inEditMode()){
              gSlashMode = true;
              addToTag(ch);
              if (event){
                 event.preventDefault();
              }
              $('#theInput').text($('#theInput').text()+ch);
              return;
          } 
      } 
      
      if (gQuoteMode){
          if (ch !== '"'){
              appendCharToCurrentCell(ch, event);
              return;
          }
      }
      
      
      if (ch === '"'){
          //console.log("got a double quote");
          if (gQuoteMode){
              var txt = current.text();
              if (endsWith(txt, '\\')){
                 current.append(ch);
                 return;
              }
              gQuoteMode = false;
              getCurrentCell().removeClass("quoted");
              //setEditMode(false);
              if (!gSkipPreKeyHook){
                  goTab();
              }
          } else {
              gQuoteMode = true;
              getCurrentCell().addClass("quoted");
          }
          //console.log("gQuoteMode: "+gQuoteMode);
          return;
      } else if ( (getCellType()===cOBJECT) && (ch === ':' ) && (!gQuoteMode) ){
          setCurrentCell(getCurrentCell().parent().find('td.valueTD')[0]);
          return;   
      } else if (ch === '['){
          if (!canAdd("array")){
              return;
          }
          //var newEl = $('<table class="arrayTABLE"><tr class="itemTR"><td class="arrayTD text"></td></tr></table>');
          var newEl = $('<table class="arrayTABLE"><tr class="itemTR"><td class="arrayTD"></td></tr></table>');
          newEl.appendTo(current);
          var theTD = newEl.find('td.arrayTD');
          var firstTD = theTD[0];
          setCurrentCell(firstTD);
      } else if (ch === '{'){
          if (!canAdd("object")){
              return;
          }
          //var newEl = $('<table class="objectTABLE"><tr class="itemTR"><td class="nameTD"></td><td class="valueTD text"></td></tr></table>');
          var newEl = $('<table class="objectTABLE"><tr class="itemTR"><td class="nameTD"></td><td class="valueTD"></td></tr></table>');
          newEl.appendTo(current);
          var theTD = newEl.find('td.nameTD');
          var firstTD = theTD[0];
          setCurrentCell(firstTD);
      } else if (ch === '}'){
          if (gLogDebug) console.log("close }");
          var TD = getCellTable_Parent(current);
          setCurrentCell(TD);
      } else if (ch === ']'){
          if (gLogDebug) console.log("close ]");
          var TD = getCellTable_Parent(current);
          setCurrentCell(TD);
      } else if (ch === ',' && (!gQuoteMode) ){
          addRow(); 
      } else {
          if (current.children("TABLE").length >0){
            addRow();   
          }
          appendCharToCurrentCell(ch, event);
      }
  }
  function appendCharToCurrentCell(ch, event){
     if (gLogDebug) console.log('processKey appending:'+ch);
          //2015: 
          getCurrentCell().append(ch); //20150406
          setEditMode(true);
          if (ch === ' '){
            if (event){
                event.preventDefault();
            }
          } 
  }
  
  function preKeyHook(ch, event){
      //debugger;
      if (gLogDebug) console.log('preKeyHook:'+ch);
      if (gSkipPreKeyHook){
          return;
      }
      if ( ! $('#ckViMode').prop('checked')){
          return false;   
      } 
      switch (ch) {
      case 'e':
      case 'r':
        //if ($('#ckEditMode').prop('checked')){
        if (inEditMode()){
          return false;   
        }    
        //$('#ckEditMode').prop('checked', true);
        setEditMode(true);
        if (ch !== 'r'){
            $('#theInput').text(getCurrentCell().text());
        } else {
            getCurrentCell().text("");   
        }
        
        if (event){
            event.preventDefault();
        }
        
        var div = $('#theInput').get(0);

        return true;  //true for handled/returnNow/eatKey. 
      case VK_ENTER:  
        //$('#ckEditMode').prop('checked', false);
        setEditMode(true);
        $('#theInput').text("");
        event.preventDefault();
        return true;
      }
      return false;     //false for notHandled/keepProcessing/keepKey
  }
  
  /* Side effect: this function will strip whitespace if you ask if you can add an array or object
   *  to a cell with only whitespace, and then tell you that you can.
   */
  function canAdd(name){
     var current = getCurrentCell();
     if (current.children().length >0){ 
         setMessage(name+" not allowed when value already contains object or array");
         return false;
     }
     if (current.hasClass('nameTD')){
         setMessage(name+" not allowed in name column");
        return false;   
     }
     if (current.text().length >0){ 
         if (current.text().trim().length==0){
            current.text("");
            return true;
         }
         setMessage(name+" not allowed when value already has text");
         return false;
     }
     
     return true;
  }

  //================== Current Cell ============================================  
  
  var currentCell;

  function getCurrentCell(){
       if (currentCell){
           return currentCell;
       }
       return $('#SDiv');
  }
  
  function setCurrentCell(element){
      if (element) {
          var aCell = (element instanceof $) ? element : $(element); 
          if (gLogDebug) console.log('setCurrentCell:'+aCell);
          if (isJsonCell(aCell)){
              currentCell = aCell;
              $('.selected').removeClass('selected');
              currentCell.addClass('selected');
              
              //this is where I'm working 2015-01-06:
              
              currentCell.attr('contenteditable','true');
              ///  currentCell.keyup(documentKeyup);
              ///  currentCell.keydown(documentKeydown);
              ///  currentCell.keypress(documentKeypress);
              //$("#theInput").detach();
              //currentCell.append($("#theInput"));
              //currentCell.append($("<p>hello</p>"));
              
              showButtons(aCell);
              //We are not showing the caret in cells anymore.
              timerSelectPosition(aCell);
              //setCommandLine(aCell.text());
              aCell.removeClass("quoted");
          } else {
              //console.log("------not setting currentCell------- element is not a JsonCell:"+dump(currentCell));
          }
      } else {
          //console.log("------setCurrentCell------- element is null");
      }
      //timerSelectPosition($('#theInput'));
  }
  
  function resetCurrentCell(){
      currentCell = null;
  }
  
  function showButtons(aCell){
  }
  
  //================== Cell Selection/Addition =================================
  
  
  function addRow(){
       var cell = getCurrentCell(); 
       if (cell && cell.get(0).nodeName === "TD"){
            var TABLE = $(cell.parents('TABLE')[0]);
            var TR;
            if (getCellType() === cOBJECT){
                TR = $('<tr class="itemTR"><td  class="nameTD"></td><td class="valueTD"></td></tr>');
            } else {
                TR =$('<tr class="itemTR"><td class="arrayTD"></td></tr>');
            }
            TR.appendTo(TABLE);
            setCurrentCell(TR.find('TD')[0]);
            //setCommandLine("");
            return;
       }
  }
  
  function isJsonCell(TD){
      //console.log("TD may be a json cell...\r\n  "+dump(TD));
      if (TD.hasClass('valueTD')){
          return true;
      } else {
          var cls = ""+TD.attr('class');
          //console.log("cls:"+cls); 
          if (cls && cls.indexOf('valueTD') > -1){
              console.log("using class search HACK: "+cls); 
              return true;
          }
      }
      if (TD.hasClass('arrayTD')){
          return true;
      }
      if (TD.hasClass('nameTD')){
          return true;
      }
      //console.log("TD was not a json cell...\r\n  "+dump(TD));
      return false;
  }
  
  function getCellType(){
        var current = getCurrentCell();
        if (current.hasClass("arrayTD")){
            return cARRAY;
        }
        if (current.hasClass("valueTD")){
            return cOBJECT;
        }
        return cOBJECT;
  }
  
  //================== Navigation ==============================================
  
  function getCellTable(cell){
      return $(cell.parents('table')[0]);
  }
  
  function getCellTable_Parent(cell){
      return $(cell.parents('table')[0]).parent();
  }
  
  function goTab(){
     setEditMode(false);
     if (getCurrentCell().hasClass("nameTD")){
         var $cell = getCurrentCell().parent().children('td.valueTD');
         var aCell = $cell[0];
         setCurrentCell(aCell); 
         return;
     }
     var $cell = getCurrentCell();//.parent().find('td.valueTD');    
     var aCell = $cell[0]; //set to valueTD cell, but override if cell contains an arrayTABLE or objectTABLE.
     
     if ($cell.children(".arrayTABLE").find(".arrayTD").length >0){
        aCell = $cell.children(".arrayTABLE").find(".arrayTD")[0];
     } else if ($cell.children(".objectTABLE").find(".nameTD").length>0){
        aCell = $cell.children(".objectTABLE").find(".nameTD")[0]; 
     }
     setCurrentCell(aCell); 
  }
  
  function goShiftTab(){
      //verbose, and early return, but works.
     setEditMode(false);
     if (getCurrentCell().hasClass("nameTD")){
         var $td = getCurrentCell().parent().parent().parent().parent();
         if ($td.length>0){
            setCurrentCell($td[0]);
            return;
         }
     }
     var nameTD = getCurrentCell().parent().children('td.nameTD')[0];
     if (nameTD){
        setCurrentCell(nameTD);
     } else {
        var $table = getCurrentCell().parent().parent().parent();
        var $container = $table.parent();
        if ($container){
            var valueTD = $container[0];
            if (valueTD){
                setCurrentCell(valueTD);
            }
        }
     }
  }
  
  function selectRowAbove(){
      setEditMode(false);
      //current might be array or object
      var current = getCurrentCell();
      if (current.hasClass('arrayTD')){
          var upTD = $(current.parents('tr')[0]).prev().children('td.arrayTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  setCurrentCell(upUpTD);
              }
          } else {
              setCurrentCell(upTD);
          }
      } else if (current.hasClass('valueTD')) {   
          var upTD = $(current.parents('tr')[0]).prev().children('td.valueTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  setCurrentCell(upUpTD);
              }
          } else {
              setCurrentCell(upTD);
          }
      } else if (current.hasClass('nameTD')) {   
          var upTD = $(current.parents('tr')[0]).prev().children('td.nameTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  setCurrentCell(upUpTD);
              }
          } else {
              setCurrentCell(upTD);
          }
      }
      //setCommandLine(getCurrentCell().text());
  }
  
  function selectRowBelow(){
      //todo: down arrow in a cell which contains an array should select first cell in array, not entire container. Similar with object, IMHO.
      setEditMode(false);
      //current might be array or object
      var current = getCurrentCell();
      if (current.hasClass('arrayTD')){
          var upTD = $(current.parents('tr')[0]).next().children('td.arrayTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  var tdBelow = upUpTD.next();
                  if (tdBelow){
                      setCurrentCell(upUpTD);
                  }
              }
          } else {
              setCurrentCell(upTD);
          }
      } else if (current.hasClass('valueTD')) {  
          var upTD = $(current.parents('tr')[0]).next().children('td.valueTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  var tdBelow = upUpTD.next();
                  if (tdBelow){
                      setCurrentCell(upUpTD);
                  }
              }
          } else {
              setCurrentCell(upTD);
          }
      } else if (current.hasClass('nameTD')) {   
          var upTD = $(current.parents('tr')[0]).next().children('td.nameTD');
          if (upTD.length===0){
              var upUpTD =  $(current.parents('tr')[0]).parent().parent().parent();
              if (upUpTD){
                  var tdBelow = upUpTD.next();
                  if (tdBelow){
                      setCurrentCell(upUpTD);
                  }
              }
          } else {
              setCurrentCell(upTD);
          }
      }
      //setCommandLine(getCurrentCell().text());
  }
  
  //============= Tag Search ===================================================
  
  var gSlashMode = false;
  
  var gTag = '';
  function getTag(){
      return gTag;
  }
  function addToTag(val){
    gTag += val;   
  }
  function clearTag(){
    gTag = '';   
  }
  function backspaceTag(){
    gTag=gTag.substring(0,gTag.length-1);  
  }

  //============= Misc =========================================================
  
  function inEditMode(){
      return $('#ckEditMode').prop('checked') === true;
  }
  
  function setEditMode(wantChecked){
      if (wantChecked){
          $('#ckEditMode').prop('checked', true);
      } else {
          $('#ckEditMode').prop('checked', false);
          gQuoteMode = false;
      }
      showButtons(getCurrentCell());
  }
  
  
  function setMessage(msg){
    $('#message').text(msg);   
  }
  
  function populate(s){
      for ( var i = 0; i < s.length; i++ ){
        var c = s.charAt(i);  
        processKey(c);
      }
  }
  
  //other options are OLD, NEW, TYPE, STRIKEOUT (during editing, after user presses 'r' for replace rather than 'e' for edit.
  
  //=============== Setting Range on contentEditable selection =================
      /*
       How do I put the focus at the end of the div instead of the front? 
       Change both instances of range.collapse(true); to range.collapse(false);
        jsfiddle.net/bq6jQ/1 
        stackoverflow.com/questions/22599694/
        See also jQuery waypoints: http://imakewebthings.com/jquery-waypoints/shortcuts/sticky-elements/
      */
  
  function timerSelectPosition(div) {
        div = div[0];
        window.setTimeout(function() {
            var sel, range;
            if (window.getSelection && document.createRange) {
                range = document.createRange();
                range.selectNodeContents(div);
                range.collapse(false);
                sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(range);
            } else if (document.body.createTextRange) {
                range = document.body.createTextRange();
                range.moveToElementText(div);
                range.collapse(false);
                range.select();
            }
        }, 2);
   }
   
  //================= JQUERY DOCUMENT READY ====================================
  
  function documentReady(){
      if (gRunTests) {
          //testJsonEditor();
          //clear();
      }
      
      //var jsonEdDiv = $('#theInput'); 
      var jsonEdDiv = getCurrentCell();
      jsonEdDiv.keyup(documentKeyup);
      jsonEdDiv.keydown(documentKeydown);
      jsonEdDiv.keypress(documentKeypress);
      
      console.log('Tagland loaded. http://tagland.org   [jQuery version: '+$.fn.jquery+']');
  }

  $(document).ready(documentReady);
 



