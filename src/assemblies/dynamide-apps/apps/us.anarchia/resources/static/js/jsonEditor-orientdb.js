   //======================= Talk to OrientDB ==================================
   
   function getSelectedDatabase(){
       return $('#selectDatabase').val();   
   }
   
   function utf8_to_b64(str) {
       return window.btoa(unescape(encodeURIComponent(str)));
   }
  
   function getOrientDBCredentials(){
       var base64str = utf8_to_b64("root:MangoDog33");  //for dev.tagland.org (and localhost as of 20140925)
       //console.log("base64:  "+base64str); 
       return base64str;
   }
   
   function ajaxSetup(){
       setMessage('');
       //not sure how often this must be called, but it is in a function, so you can call it from other functions for saftety.
       $.ajaxSetup({
            xhrFields: {
                withCredentials: false
            },
            crossDomain: true
        });
   }
   
   function ajaxErrorLogger(error){
        // Log any error.
        var msg = JSON.stringify(error);
        console.log("ERROR message:", msg);
        console.log("ERROR object:", error);
        setMessage(msg);
   }
   
   function ridToQueryParam(rid){
        if (startsWith(rid, "#")){
            rid = rid.substring(1);
        }
        return rid;
   }
   
   var gLastRid = '3:0';
   function setLastRid(val){
       gLastRid = val;
   }
   function getLastRid(){
       if (gLastRid.indexOf("#") === 0){
           return gLastRid.substring(1);
       }
       return gLastRid;
   }
   
   function boldTagInPath(path, parentPath){
       if (parentPath){
            var arr = (new RegExp('(^'+parentPath+')(.*)')).exec(path);
            if (arr && arr.length == 3){
                //regex: [0] is the whole string matched, [1] is the match of group 1, [2] is the match of group 2.
                var n = arr[2];
                if (n[0] === '/'){
                    path = '.' + n;
                } else {
                    path = './' + n;
                }
            }
       }
       var slash = path.lastIndexOf('/');
       var res = path.substring(0,slash+1) + '<span class=\'tagname\'>'+path.substring(slash+1)+'</span>';
       return res;
   }
   //========================= ajax calls ======================================
   
   function ajaxQueryDocList(){
       ajaxSetup();
       var selectedDatabase = getSelectedDatabase();
       $.ajax({
            type: 'GET',
            url: selectedDatabase + ':2480/query/tagonomy/sql/select @rid as realrid, name from Doc' ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                $('#jsonOutput').text(JSON.stringify(response));
                window.lastJSONResponse = response;
                var lines = [];
                lines.push('<h6>server: '+selectedDatabase+'</h6>');
                for (key in response.result){
                    var realrid = response.result[key]["realrid"];
                    var name = response.result[key]["name"];
                    var line = '<div><a href=\"javascript:getDocument(\''+realrid+'\')\">'+name+'</a>'
                                +'&nbsp;&nbsp;<a style=\"float:right;\" href=\"javascript:deleteDocument(\''+realrid+'\')\">[x]</a></div>';
                    lines.push(line);
                }
                $('#divDocList').html(lines.join('\n'));
            },
            error: ajaxErrorLogger
       })
   }
   
   function ajaxCall_tagsForDoc(docRid){
       ajaxSetup();
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/function/tagonomy/tagsForDoc/'+docRid ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                $('#jsonOutput').text(JSON.stringify(response));
                var lines = [];
                for (key in response.result){
                    var row = response.result[key];
                    var path =   row["path"];
                    var name =   row["name"];
                    var tagrid = row["tagrid"];
                    var line = '<div><a href=\"javascript:getDocument(\''+tagrid+'\')\"><span class="tag">'+boldTagInPath(path)+'</span></a></div>';
                    lines.push(line);
                }
                $('#divTagsForDoc').html(lines.join('\n'));
            },
            error: ajaxErrorLogger
       })
   }
   
   function ajaxCall_tagsByPartialPath(pathStr){
       
       var query = "select path,name, @rid as tagrid from Tag where path like '%" + pathStr + "%'";
       
       
       ajaxSetup();
       $.ajax({
            //type: 'POST',
            //url: getSelectedDatabase() + ':2480/function/tagonomy/tagsByPartialPath/'+encodeURIComponent(pathStr) ,
            type: 'GET',
            url: getSelectedDatabase() + ':2480/query/tagonomy/sql/'+encodeURIComponent(query) ,
            
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                $('#jsonOutput').text(JSON.stringify(response));
                var i = 0;
                var lines = [];
                for (key in response.result){
                    var row = response.result[key];
                    var path =   row["path"];
                    var name =   row["name"];
                    var tagrid = row["tagrid"];
                    var tagsArray = tagrid;//just one element for now.
                    var line = '<div>'
                                  +'<input type="checkbox" id="cbTag_'+i+'" data-rid="'+tagrid+'"></input>'
                                  +'<a href="javascript:getDocument(\''+tagrid+'\')">'
                                  +'<span class="tag">'+boldTagInPath(path)+'</span>'
                                  +'</a>'
                                  +'&nbsp;&nbsp;&nbsp;&nbsp;'
                                  +'<a href="javascript:linkCallDocsForTags(\''+tagsArray+'\')">docs'    //+tagsArray
                                  +'</a>&nbsp;&nbsp;&nbsp;'
                                  +'<span style="float: right"><a href=\"javascript:tagDocument(\''+tagrid+'\')\"><span class=\"verbLink\">Apply Tag</b></a></span>&nbsp;&nbsp;&nbsp;&nbsp;'
                              +'</div>';
                    lines.push(line);
                    i++;
                }
                $('#divTagsByPartialPath').html(lines.join('\n'));
            },
            error: ajaxErrorLogger
       })
   }
   
   function ajaxCall_createTag(newName, parentRid){
       var parentGremlinId = ridToQueryParam(parentRid);
       ajaxSetup();
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/function/tagonomy/createTag/'+newName+'/'+parentGremlinId ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                $('#jsonOutput').text(JSON.stringify(response));
                ajaxGetDocument(parentGremlinId);
                //$('#divTagsByPartialPath').html(lines.join('\n'));
            },
            error: ajaxErrorLogger
       })
   }
   
   function tagDocument(tagRID){
        var docRID = '#'+getLastRid();
        ajaxCmd_tagDoc(tagRID, docRID);
        getDocument(docRID);
   }
   
   /*
            Doc ---taggedby--> Tag
   
   */
   
   
   // tagRID and docRID should have the # sign, e.g. ajaxCmd_tagDoc('#15:0','#12:6');
   function ajaxCmd_tagDoc(tagRID, docRID){
       ajaxSetup();
       
       var cmdText = "create edge TaggedBy from "+docRID+" to "+tagRID;
       console.log("ajaxCmd_tagDoc: "+cmdText);
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/command/tagonomy/sql/'+encodeURIComponent(cmdText) ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                console.log("ajaxCmd_tagDoc: orientdb raw: "+JSON.stringify(response));
            },
            error: ajaxErrorLogger
       })
   }
   
   function linkCallDocsForTags(tagsCSV){
       ajaxQuery_DocsForTags(tagsCSV, '#divDocsForTags', '.docsfortag_title');
   }
   
   function ajaxQuery_DocsForTags(tagsCSV, ouputSelector, titleSelector){
       ajaxSetup();
       //var tags = tagsArray.join(',');
       //var cmdText = "select expand(in('TaggedBy')) from ["+tagsCSV+"]";
       //This worked, but had duplicates:
       var cmdText = "select @rid as docrid, name, url from (select expand(in('TaggedBy')) from ["+tagsCSV+"])";
       //This works, still has duplicates, but now shows the tags:
       var cmdText = "select @rid as docrid, name, url, out('TaggedBy').path as tags from (select expand(in('TaggedBy')) from ["+tagsCSV+"])";
       console.log("ajaxCmd_tagDoc: "+cmdText);
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/command/tagonomy/sql/'+encodeURIComponent(cmdText) ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                console.log("ajaxQuery_DocsForTags: orientdb raw: "+JSON.stringify(response));
                var lines = [];
                var uniqueDocs = [];
                for (key in response.result){
                    var row = response.result[key];
                    
                    var docrid = row["docrid"];
                    if (uniqueDocs.indexOf(docrid)>-1){
                        continue;  //Duplicate row from server query query response.  Skip it.
                    }
                    uniqueDocs.push(docrid);
                    
                    var name =   row["name"];
                    var url =    row["url"];
                    var tags =   row["tags"];
                    
                    var tb = [];
                    for (var k in tags){
                        tb.push('<span class="tag">' +boldTagInPath(tags[k]) + '</span>');
                    }
                    var line = '<div><a href=\"javascript:getDocument(\''+docrid+'\')\"><b>'+name+'</b></a>'
                                  //url not defined in docs yet.  +'&nbsp;&nbsp;&nbsp;<a target="_blank" href="'+url+'">'+url+'</a>'
                                  +'&nbsp;&nbsp;&nbsp;'+tb.join('&nbsp;&nbsp;')
                              +'</div>';
                    lines.push(line);
                }
                $(ouputSelector).html(lines.join('\n'));
                $(titleSelector).html(tagsCSV);
            },
            error: ajaxErrorLogger
       })
   }
   //select expand(in('TaggedBy')) from [#12:12, #12:13]
   
   function ajaxQuery_SearchDocsByTerm(searchStr, ouputSelector){
       ajaxSetup();
       var cmdText = "select @rid as docrid, @version, name, url, doc, posted, mimetype  "
                    +"from Doc where any() like '%"+searchStr+"%'";
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/command/tagonomy/sql/'+encodeURIComponent(cmdText) ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                var lines = [];
                for (key in response.result){
                    var row = response.result[key];
                    var docrid = row["docrid"];
                    var name =   row["name"];
                    var url =    row["url"];
                    var doc =   row["doc"];
                    if (doc){
                        doc = doc.substring(0, 40);
                    }
                    var line = '<div><a href=\"javascript:getDocument(\''+docrid+'\')\"><b>'+name+'</b></a>'
                                  +'&nbsp;&nbsp;&nbsp;<a target="_blank" href="'+url+'">'+url+'</a>'
                                  +'&nbsp;&nbsp;&nbsp;'+name
                              +'</div>';
                    lines.push(line);
                }
                $(ouputSelector).html(lines.join('\n'));
            },
            error: ajaxErrorLogger
       })
   }
   
   //================== ajax CRUD ==============================================
   
   function ajaxQuery(queryStr, successFunction){
       ajaxSetup();
       $.ajax({
            type: 'POST',
            url: getSelectedDatabase() + ':2480/command/tagonomy/sql/'+encodeURIComponent(queryStr),
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: successFunction,
            error: ajaxErrorLogger
       })
   }
   
   
   function ajaxGetDocument(rid){
       ajaxSetup();
       $.ajax({
            type: 'GET',
            url: getSelectedDatabase() + ':2480/document/tagonomy/'+rid ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                clear();
                console.log("orientdb raw: "+JSON.stringify(response));
                populate(JSON.stringify(response));
                setLastRid(rid);
                ajaxCall_tagsForDoc(ridToQueryParam(rid));
                showMenuAndObjects(response);
            },
            error: ajaxErrorLogger
       })
   }
   
   function ajaxPutDocument(postData){
        ajaxSetup();
        $.ajax({
            type: 'PUT',
            data: JSON.stringify(postData),
            url: getSelectedDatabase() + ':2480/document/tagonomy',
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                console.log("orientdb response: "+dumpr(response, 0));
                var oRes = JSON.parse(response);
                var rid = oRes['@rid'];
                console.log("@rid from response: "+rid);
                setLastRid(rid);
                setMessage("document saved. @rid = "+rid);
                ajaxQueryDocList();
                getDocument(rid);//db may have changed version field. reload.
            },
            error: ajaxErrorLogger
        })
   }
   
   function ajaxPostDocument(postData){
        postData['@class'] = 'Doc';  //new doc, we have to tell it which class to use.
        ajaxSetup();
        $.ajax({
            type: 'POST',
            data: JSON.stringify(postData),
            url: getSelectedDatabase() + ':2480/document/tagonomy',
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                console.log("orientdb response: "+dumpr(response, 0));
                var oRes = JSON.parse(response);
                var rid = oRes['@rid'];
                console.log("@rid from response: "+rid);
                setLastRid(rid);
                setMessage("document saved. @rid = "+rid);
                ajaxQueryDocList();
                getDocument(rid);//db may have changed version field. reload.
            },
            error: ajaxErrorLogger
        })
   }
   
   function ajaxDeleteDocument(rid){
       ajaxSetup();
       $.ajax({
            type: 'DELETE',
            url: getSelectedDatabase() + ':2480/document/tagonomy/'+rid,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                clear();
                setLastRid('');
                console.log("orientdb raw: "+JSON.stringify(response));
                ajaxQueryDocList();
            },
            error: ajaxErrorLogger
       })
   }
   
  //============================================================================

   function testOrientDBStorage(){
      var postData = {"todo": "sleep", "books":[{"title":"A Catcher in the Rye"},{"title":"Huckleberry Finn", "author":"Mark Twain"}]};
      ajaxPostDocument(postData);
      ajaxGetDocument("#3:69");
   }
   
   function postDocument(){
      var postData = saveToJson();
      console.log("postData:"+postData);
      ajaxPostDocument(JSON.parse(postData));
   }
   
   function putDocument(){
      var putData = saveToJson();
      console.log("putData:"+putData);
      ajaxPutDocument(JSON.parse(putData));
   }
   
   function getDocument(rid){
      rid = ridToQueryParam(rid);  //REST API doesn't like the # in an @rid: "#15:0" so strip off the "#":
      ajaxGetDocument(rid); 
   }
   
   function deleteDocument(rid){
      rid = ridToQueryParam(rid);
      ajaxDeleteDocument(rid);  
   }
   
   function addTagChild(inputSelector, parentRid){
       console.log('inputSelector: '+inputSelector+' parentRid:'+parentRid);
        var newName = $(inputSelector).val();
        if (newName.trim().length < 1){
            console.log("addTagChild refused to create tag with empty name.");
            return;
        }
        ajaxCall_createTag(newName, parentRid);
   }
   
   function saveUserPrefs(userID){
       var query = "select from Doc where meta.doctype = 'preferences' and meta.userid = '"+userID+"'";
       ajaxQuery(query, function(result){
           if (result.result && result.result[0] && result.result[0].doc){
                var prefs = result.result[0].doc;
                prefs.showLog = ""+$('#ckViewLog').prop('checked');
                prefs.showSearch = ""+$('#ckViewSearch').prop('checked');
                ajaxPutDocument(result.result[0]);
           }
       });       
   }
   
   function getUserPrefs(userID){
       var query = "select from Doc where meta.doctype = 'preferences' and meta.userid = '"+userID+"'";
       ajaxQuery(query, function(result){
            console.log("getUserPrefs: "+JSON.stringify(result));
            if (result.result && result.result[0] && result.result[0].doc){
                var prefs = result.result[0].doc;
                
                if (prefs.showLog == "true"){
                   $('#ckViewLog').prop('checked', true);
                   $('#dragLog').show();  
                } else {
                   $('#ckViewLog').prop('checked', false);
                   $('#dragLog').hide();  
                }
                
                if (prefs.showSearch == "true"){
                   $('#ckViewSearch').prop('checked', true);
                   $('#dragDocsForTag').show();  
                } else {
                   $('#ckViewSearch').prop('checked', false);
                   $('#dragDocsForTag').hide();  
                }
            }
       });
   }
   
   function getTodoTemplate(){
       getTemplate('org.tagland.type.TodoTemplate');
   }
   
   function getTemplate(templateDoctype){
       var query = "select from Doc where meta.doctype = '"+templateDoctype+"'";
       ajaxQuery(query, function(result){ 
           if (result.result && result.result[0] && result.result[0].doc){
               var doc = result.result[0].doc;
               console.log('Template: '+JSON.stringify(doc));
               //New document.
               clear();
               populate(JSON.stringify(doc));
           }
       });
   }
   
   function hidePopup(selector){
        $(selector).hide();   
   }
   
   function showEditor(doctype, model, atRid){
       //showEditor("org.tagland.type.doc.todo", result, atRid);
       var html = layoutTodoEditor(model);
        $('#todoEditor').html(html);
        $('#todoEditor').show();   
   }
   function hideEditor(){
       $('#todoEditor').hide();
   }
   
   function layoutTodoEditor(model){
        var h = "";
        h += "<form name='todoEditorForm'>";
        h += "<input class='todoformctrl' id='todo_name' value='"+model.name+"' size='120' /><br />";
        h += "<textarea class='todoformctrl' id='todo_description'>"+model.doc.description+"</textarea><br />";
        var deps = model.doc.dependencies;
        h += "<select class='todoformctrl'  id='todo_dependencies'>";
        for (var key in deps){
                h += "<option value='"+key+"'>"+deps[key]+"</option>";
        }
        h += "</select>";
        h += "</form>";
        return h;
    }
       
    function popupQueryResultRaw(atRid, name, query, popupSelector, path){  //path is optional parameter: if present, passes to boldTagInPath, which strips path of parent from current.
        ajaxSetup();
       $.ajax({
            type: 'GET',
            url: getSelectedDatabase() + ':2480/command/tagonomy/sql/'+encodeURIComponent(query) ,
            headers: {"Authorization": "Basic "+getOrientDBCredentials() },
            success: function (response) {
                var br = "<BR />\n"
                console.log(" query: "+query+" orientdb raw: "+JSON.stringify(response));
                
                var lines = [];
                var line = "";
                for (var i in response.result){
                    var row = response.result[i]; 
                    var pathDecorated = boldTagInPath(row.path, path);
                    line = "<a class='tag' href='javascript:getDocument(\""+row['realrid']+"\");'>"+pathDecorated+"</a>";
                    lines.push(line);
                }
                $(popupSelector).html(lines.join(br)+br)
                                .show();
            },
            error: ajaxErrorLogger
       }) 
    }
    
    function showMenuAndObjects(result){
       if (result && result["@class"] && (result["@class"] == "Tag")){
            var lines = [];
            var atRid =    result["@rid"];
            var atClass =  result["@class"];
            var name =     result["name"];
            var path =     result["path"];
            
            //This is the display of the current tag in the popupTree widget.
            var parPath = path.substring(0, path.lastIndexOf('/'));
            $('.tagcurrent').html(boldTagInPath(path, parPath));
            
            var queryChildren = "select @rid as realrid, name, path from (select expand(out('TagChild')) from ["+atRid+"])"
            var queryParent =   "select @rid as realrid, name, path from (select expand(in('TagChild')) from ["+atRid+"])";
            popupQueryResultRaw(atRid, name, queryParent, '.popupParent'); 
            popupQueryResultRaw(atRid, name, queryChildren, '.popupChildren', path); 
            
            lines.push("<input type='text' value='' id='inputNewTagChild' />");            
            lines.push("<a href='javascript:addTagChild(\"#inputNewTagChild\",\""+atRid+"\");'>&nbsp;&nbsp;<span class='instrument shadow4 smallbtntext'>add</span></a>");
            var addChildTagCtl = lines.join('');
            //$('#docMenu').html(addChildTagCtl).show();
            $('#tagTreeMenu').html(addChildTagCtl).show();
            var tagsCSV = atRid;
            $('.docsfortag2_title').show(); 
            $('#popupDocsForTag2').show(); 
            $('#popupTree').show(); 
            $('#tagsForDocument').hide();
            hideEditor();
            ajaxQuery_DocsForTags(tagsCSV, '.divDocsForTags2', '.docsfortag2_title');
       } else if (result && (result["@class"] == "Doc") && (result["doctype"] == "org.tagland.type.doc.todo")) {
           console.log("in doctype == org.tagland.type.doc.todo");
           showEditor("org.tagland.type.doc.todo", result, atRid);
       } else {
           hideEditor();
           $('#docMenu').html('').show();
           $('.divDocsForTags2').html(''); 
           hidePopup('.docsfortag2_title'); 
           hidePopup('#popupDocsForTag2');
           hidePopup('.popupParent');
           hidePopup('.popupChildren');
           hidePopup('#popupTree');
            $('#tagsForDocument').show(); 
       }
   }
   
        
   

