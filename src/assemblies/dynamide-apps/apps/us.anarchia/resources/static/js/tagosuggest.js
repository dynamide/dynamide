          var VK_BAK = 8;
          var VK_QUESTION_SLASH = 191;
          var VK_SHIFT = 16;
          var VK_SPACE = 32;
          
          var gLogDebug = false; 
          
          var lastSearch = "";
          var gSpaceKeyMode = false;
          var gSuggestions = [];
          var kcount = 1;
          var gNotesArrayIndex = 0;
          var gNotesArray = [];
          function setNotesArray(arr){
                gNotesArray = arr;
          }
          function endsWith(str, suffix) {
            return str.indexOf(suffix, str.length - suffix.length) !== -1;
          }
          
          function trapDefault(event){
                if (gLogDebug) console.log("prevent default...");
                event.preventDefault();  
          }
          
          
         
          function documentKeyup(event){
               var shifted = event.shiftKey && event.shiftKey == true;
               var key = event.which;
               if ((key == VK_QUESTION_SLASH && shifted) || key == '?'){ 
                    trapDefault(event);
                    toggleHelp();
                    $('#tag').val("");
                    $('#tag').focus();
                   return;
                }
          }

          function processKey(event){
                    kcount++;
                    var shifted = event.shiftKey && event.shiftKey == true;
                    var ch = $('#tag').val();
                    var $b = $('#buffer');
                    var cur = $b.text();
                    var key = event.which;
                    if (gLogDebug) console.log('keyup:'+key);
                    
                    switch ( event.which  ){
                    case VK_BAK: 
                        trapDefault(event);
                        hideHelp();
                        cur=cur.substring(0,cur.length-1);
                        $b.text(cur);
                        getAutocomplete($b.text());
                        return false;
                    case VK_SPACE:
                        gSpaceKeyMode = true;
                        if (gLogDebug) console.log("space key mode...");
                        $('#buffer').css("background-color", "lightblue");
                        checkForLocalCmd(cur);
                        return;
                        break;
                    default:
                    } 
                    
                    if (ch ==='?'){
                        if (gLogDebug) console.log("caught late shift key: "+event.key + ','+event.shiftKey+','+key+" kcount: "+kcount);
                        return;
                    }
                    
                    if (gSpaceKeyMode){
                        $('#buffer').css("background-color", "");
                        gSpaceKeyMode = false;
                        ch = ch.trim();
                        if (ch=='s'){
                            save(cur);
                            $('#tag').val("");
                            ch = '';
                        } else if (! isNaN(ch)){
                            if (gLogDebug) console.log("found digit:"+ch);
                            var found = gSuggestions[ch];
                            if (found){
                                $('#buffer').text(found);
                            }
                            $('#tag').val("");
                            return;   
                        }
                    }
                    var fullCmd  = cur + ch;
                    $b.text(fullCmd);
                    $('#tag').val("");
                    if (ch == "/"){
                          checkForLocalCmd(fullCmd); 
                    }
                getAutocomplete($b.text());
          }

          function getAutocomplete(tag){
              //console.log("tag: "+tag);
              if (tag && tag.length>0){
              } else {
                $('#outEl').html("");
                console.log("null or missing tag");
                return;
              }
              if (lastSearch===tag){
                console.log("Continuing, but: Repeated search, skipping server. lastSearch: "+lastSearch+" tag: "+tag);
                //return;
              }
              if (endsWith(tag," s")){
                console.log("SAVING...");
                save(tag.substring(0, tag.length-2));
                return;
              }
              if (gLogDebug) console.log("tag search going to server: "+tag);
              lastSearch = tag;
              // AJAX CALL HERE :
              $.get('/tagonomy?tag='+tag,
                  function(data) {
                      //console.log("data: "+data);
                      var autosuggestArray = eval('(' + data + ')');
                      $('#outEl').html("");
                      setAutocompleteResult(autosuggestArray.autosuggest);
              });
          }

          function setAutocompleteResult(suggestions){
            var $outEl = $('#outEl'); 
            if (suggestions){
                gSuggestions = [];
                //console.log("JSON: "+JSON.stringify(suggestions));
                $.each(suggestions, function(i, sugg){
                      var cell = ' <div class="suggestRow">'+i+' <a href="'+sugg.url+'">'+sugg.path+'</a> ('+sugg.space+')</div';
                      $outEl.append(cell);
                      gSuggestions.push(sugg.path);
                });
            } else {
                 $outEl.html("");
            }
          }
          
          function save(tag){
              var space = $('#space').val();
              // AJAX CALL HERE :
              $.get('/tagonomy?savetag='+tag+'&space='+encodeURIComponent(space),
                  function(data) {
                      console.log("save() AJAX returned data: "+data);
                      //var result = eval('(' + data + ')');
                      $('#outEl').html(""+data);
              });
          }
          
          function getspaces(user){
              // AJAX CALL HERE :
              $.get('/tagonomy?getspaces='+user,
                  function(data) {
                      console.log("save() AJAX returned data: "+data);
                      var spacesRes = eval('(' + data + ')');
                      for (var el in spacesRes.spaces) {
                            console.log("space: "+spacesRes.spaces[el].space);  
                            $('#spacesOL').append("<li>"+spacesRes.spaces[el].space+"</li>");
                      }
              });
          }
          
          //============ Commands ========================================
          
          var rootspace = [];
          rootspace['1'] = {
              'id':'1',
              'shortcut':'s','caption':'spaces',
              'cmd': function(tools,terms){
                   tools.print('in command 1. terms: '+terms);
                   if (terms && terms.length>0){
                       var term = terms[0];
                       if (term){
                           tools.highlight(term.trim());
                       }
                   }
              }          
          };
          rootspace['2'] = {'id':'2','shortcut':'a','caption':'aliases'};
          
          function checkForLocalCmd(curr){
              //debugger;
                //eg "/p1/ch2/ch3";
                var arrVars = curr.split("/");
                var firstVar = arrVars.shift();
                var b;
                while (b = arrVars.shift()){
                    b = b.trim();
                    if (b.length>0){
                        if (rootspace[b]) {
                            console.log("found command: "+firstVar+" :: "+rootspace[b]); 
                            
                            highlightSpace(b);
                            sandbox(rootspace[b], arrVars);
                        }
                    }
                }
          }
          
          function sandbox(command, terms){
               var tools = {
                'print':function(msg){console.log(msg);},
                'highlight':function(idx){highlight(idx);}
               }
               //if the command.cmd is not an object, then eval it.  but rootspace is local, so object is defined.  var obj = eval('(' + cmd + ')'); 
               if (command.cmd){
                    command.cmd(tools, terms);   
               }
          }
          
          function highlightSpace(idx){
              console.log("highlightSpace");
               $('.spaceboxcaption').addClass('highlight'); //using CLASS will highlight ALL such captions.  todo: change to ID.
          }
          function unhighlightSpace(){
              console.log("unhighlightSpace");
               $('.spaceboxcaption').removeClass('highlight'); //using CLASS will highlight ALL such captions.  todo: change to ID.
          }
         
          function highlight(idx){
              unhighlightSpace();
            console.log("highlighting "+idx+"....."); 
            $('#spacesOL li').removeClass('highlight');
            var $spaceLI = $('#spacesOL li:nth-child('+idx+')');
            $spaceLI.addClass('highlight'); 
            $('#space').val($spaceLI.html());
          }
              
          //====================================================
          
          function toggleHelp(){
                if ( $('#help').is(':visible') ) {
                      $('#help').hide();
                      return;
                }
                var pos = $('#tag').offset();    
                var eWidth = $('#tag').outerWidth();
                var mWidth = $('#help').outerWidth();
                var left = (pos.left + eWidth - mWidth) + "px";
                var top = 3+pos.top+$('#tag').height() + "px";
                //show the menu directly over the placeholder  
                $('#help').css( { 
                    position: 'absolute',
                    zIndex: 5000,
                    left: left, 
                    top: top
                } );

	      $('#help').show();
              if (gLogDebug) console.log('toggle() help...');
          }
          
          function hideHelp(){
              $('#help').hide();
              if (gLogDebug) console.log('hiding help...');
          }
          
          //================= JQUERY DOCUMENT READY ================
          function documentReady(){
               $("input#tag").keyup(processKey);

                // Create the Ajax event listeners for global pub/sub:
                $("#loading").bind("ajaxSend", function(){
                        $(this).stop(false, true).effect('highlight', {color: 'yellow'}, 1000);
                        $(this).show();
                });

                $("#loadingDone").bind("ajaxComplete", function(){
                        $(this).stop(false, true).effect('highlight', {color: '#8DD2F7'}, 1000);
                });

                $(document).keyup(documentKeyup);
                getspaces('laramie');
                hideHelp();
                $('#tag').focus();
          }

          $(document).ready(documentReady);
