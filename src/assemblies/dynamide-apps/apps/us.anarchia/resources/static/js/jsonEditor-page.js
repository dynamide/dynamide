    function theInput_click(){
        $('#theInput').focus();
    }
    function btnR_click(){
        goTab();
        $('#theInput').focus();
    }
    function btnL_click(){
        goShiftTab();
        $('#theInput').focus();
    }
    function btnR_click(){
        goTab();
        $('#theInput').focus();
    }
    function btnUp_click(){
        selectRowAbove();
        $('#theInput').focus();
    }
    function btnDown_click(){
        selectRowBelow();
        $('#theInput').focus();
    }
    function btnColon_click(){
        processKey(':');
        $('#theInput').focus();
    }
    function btnLCurly_click(){
        processKey('{');
        $('#theInput').focus();
    }
    function btnRCurly_click(){
        processKey('}');
        $('#theInput').focus();
    }
    function btnLSquare_click(){
        processKey('[');
        $('#theInput').focus();
    }
    function btnRSquare_click(){
        processKey(']');
        $('#theInput').focus();
    }
    function btnComma_click(){
        processKey(',');
        $('#theInput').focus();
    }
    function btnEnter_click(){
        addRow(); 
        $('#theInput').focus();
    }
    function btnJson_click(){
        var json = saveToJson(); 
        //console.log("json as saved:"+json);
        var obj = JSON.parse(json);
        //console.log("json as parsed:"+obj);
        var json2 = JSON.stringify(obj, null, 0);
        //console.log("json2 as stringified:"+json2);
        $('#jsonOutput').val(json2);
    }
    function ckEditMode_click(){
        $('#theInput').focus();
    }
    function btnPOST_click(){
       postDocument(); 
    }
    function btnPUT_click(){
       putDocument(); 
    }
    function btnLoad_click(){
       ajaxQueryDocList();
    }
    function btnNew_click(){
       clear();
       gSkipPreKeyHook = true;
       populate('{name:"New Doc"}');
       gSkipPreKeyHook = false;
    }
    
    function menubtnArrows_click(){
       $('#mnubtnCanvas').show(); 
    }
    
    var lastSearch='',
        gSuggestions = [];;
    
    function getAutocomplete(tag){
          if (tag && tag.length>0){
          } else { 
            console.log("null or missing tag");
            return;
          }
          if (lastSearch===tag){
            console.log("Continuing, but: Repeated search, skipping server. lastSearch: "+lastSearch+" tag: "+tag);
            //return;
          }
          if (gLogDebug) console.log("tag search going to server: "+tag);
          lastSearch = tag;
          // AJAX CALL HERE :
          $.get('/tagonomy?tag='+tag,
              function(data) {
                  //console.log("data: "+data);
                  var autosuggestArray = eval('(' + data + ')');
                  setAutocompleteResult(autosuggestArray.autosuggest);
          });
      }
    
      function setAutocompleteResult(suggestions){
        var $outEl = $('#outEl'); 
        $outEl.html("");
        if (suggestions){
            gSuggestions = [];
            //console.log("JSON: "+JSON.stringify(suggestions));
            $.each(suggestions, function(i, sugg){
                  var cell = ' <div class="suggestRow">'+i+' <a href="'+sugg.url+'">'+sugg.path+'</a> ('+sugg.space+')</div';
                  $outEl.append(cell);
                  gSuggestions.push(sugg.path);
            });
        }
      }
    
