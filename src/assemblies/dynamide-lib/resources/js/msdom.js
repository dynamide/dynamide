// **************************************************************************
// *******   IMPORTANT: Depends on scripts.js, so include that first.  ******
// **************************************************************************
var scripts_js_included;
if (!scripts_js_included) {
    alert('ERROR: msdom.js depends on scripts.js, which was not included');
}


  // You can test this file with the page:
  //  /com/dynamide.experimental/ie/test-dom-diver.html

  //from http://msdn.microsoft.com/library/default.asp?url=/library/en-us/xmlsdk30/htm/xmpronodetype.asp
  //MS DOM nodeType:
  var NODE_TAG = 1;  //NODE_ELEMENT
  var NODE_ATTRIBUTE = 2;
  var NODE_TEXT = 3;
  var NODE_CDATA_SECTION = 4;
  var NODE_ENTITY_REFERENCE = 5;
  var NODE_ENTITY = 6;
  var NODE_PROCESSING_INSTRUCTION = 7;
  var NODE_COMMENT = 8;
  var NODE_DOCUMENT = 9;
  var NODE_DOCUMENT_TYPE = 10;
  var NODE_DOCUMENT_FRAGMENT = 11;
  var NODE_NOTATION = 112;

  //see more at com\dynamide.experimental\dom\domapi\src\core.js
  var isDom  = document.getElementById?1:0;
  var isIE   = (navigator.userAgent.indexOf("MSIE" )>0)?1:0;
  var isNS   = (navigator.userAgent.indexOf("Gecko")>0)?1:0;
  var isMac  = (navigator.userAgent.indexOf("Mac"  )>0)?1:0; //  <----- can someone verify this?
  var isIE5  = 0;
  var isIE50 = 0;
  var isIE55 = 0;
  var isIE60 = 0;
  if(isIE){
    var i    = navigator.appVersion.indexOf("MSIE");
    var temp = navigator.appVersion.substring(i+5,i+8);
    isIE50   = (temp=="5.0");
    isIE55   = (temp=="5.5");
    isIE60   = (temp=="6.0");
    isIE6    = (temp=="6." );
    isIE5    = isIE50||isIE55;
  };
  var isIElt6 = 0;
  if (isIE5 || isIE50 || isIE55) {
    isIElt6 = 1;
  }
  //alert("isIElt6: "+isIElt6);


  //=========== GET a document in the background =================

  function loadXMLResource(resource){
      var root = loadDocument(resource);
      if ( root == null ) {
           return "ERROR: [loadXMLResource] root is null. resource: "+resource;
      } else if ( root.nodeName == "error" ) {
           return "ERROR: in loadXMLResource (root.nodeName == 'error'). resource: "+resource;
      }
      return domToString(root);
  }
  
  function loadDocument(resource){
    function reqListener () {
      //console.log(" this.responseXML ====>");
      //console.log(this.responseXML);
      //console.log(" this.responseText ====>");
      //console.log(this.responseText);
    };
    var oReq = new XMLHttpRequest();
    oReq.onload = reqListener;
    oReq.open("get", resource, false);  //3rd arg: true==async.  We want false, because dynamide ide was not written with js closures, so some code is synchronous.
    oReq.send(); 
    if (oReq.responseText){
        //console.log("oReq.responseText:"+ oReq.responseText);
        return oReq.responseText;   
    } else if (oReq.responseXML){
        //console.log("oReq.responseXML:"+ oReq.responseXML);
        return oReq.responseXML;   
    }
  }
  
  //=========== POST a document in the background =================
  
  function postXMLResource(url, content){
      var root = postDocument(url, content);
      if ( root == null ) {
           return "ERROR: [postXMLResource] root is null. url: "+url;
      } else if ( root.nodeName == "error" ) {
           return "ERROR: in postXMLResource (root.nodeName == 'error'). url: "+url;
      }
      return domToString(root);
  }
  
  function postDocument(url, content){
    function reqListener () {
      //console.log(" this.responseXML ====>");
      //console.log(this.responseXML);
      //console.log(" this.responseText ====>");
      //console.log(this.responseText);
    };
    var oReq = new XMLHttpRequest();
    oReq.onload = reqListener;
    oReq.open("POST", url, false);  //3rd arg: true==async.  We want false, because dynamide ide was not written with js closures, so some code is synchronous.
    oReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    oReq.setRequestHeader("Content-length", content.length);
    oReq.setRequestHeader("Connection", "close");
    oReq.send(content); 
    if (oReq.responseText){
        //console.log("oReq.responseText:"+ oReq.responseText);
        return oReq.responseText;   
    } else if (oReq.responseXML){
        //console.log("oReq.responseXML:"+ oReq.responseXML);
        return oReq.responseXML;   
    }
  }

  function loadDocumentActiveX(resource){
      //Microsoft.XMLDOM is of type IXMLDOMDocument
      var xmlDoc=new ActiveXObject("Microsoft.XMLDOM")
      xmlDoc.async=false
      xmlDoc.load(resource)
      root = xmlDoc.documentElement   //type is IDomDocument
      if (root == null){
          alert('Document was not found or was not well-formed.'
          +'\r\n  reason: '     + xmlDoc.parseError.reason
          + '  line: '          + xmlDoc.parseError.line
          + '\r\n  linepos: '   + xmlDoc.parseError.linepos
          + '\r\n  filepos: '   + xmlDoc.parseError.filepos
          + '\r\n  errorCode: ' + xmlDoc.parseError.errorCode
          + '\r\n  url: '       + xmlDoc.parseError.url
          + '\r\n\r\n  srcText: '   + xmlDoc.parseError.srcText
          );
      }
      return root;
  }

  function domToString(root){
    if ( root == null ) {
        alert("ERROR: [domToString] document root is null");
        return "";
    }
    //pass true for the entry function, for the parameter isRoot.  This omits the root,
    // which is usually just a container.
    var newHTML=domToStringDiver(root, true)
    //alert(newHTML);
    logDebug("<pre>"+newHTML.replace(/</g, "&lt;")+"</pre>");
    return newHTML;
  }

    //Note: I don't support processing instructions, etc.
  function domToStringDiver(passedNode, isRoot){
    var html = '';
    var typ = typeof passedNode;
    if (typ === "string" && passedNode.nodeType === undefined){
        return passedNode;
    }
    console.log(" domToStringDiver ==> "+typ+"="+passedNode.nodeType +":"+ passedNode.nodeName+":"+passedNode.text );
    if (passedNode.nodeType == NODE_TEXT){
        if (!isRoot && passedNode.text != undefined)    html += passedNode.text;
    } else if (passedNode.nodeType == NODE_COMMENT && passedNode.text != undefined){
        if (!isRoot)
            if (isIElt6){
  //alert("true, isIElt6: "+isIElt6+" "+passedNode.text);
                html += "<!--" + passedNode.text + "-->";
            } else {
  //alert("false isIElt6: "+isIElt6+" "+passedNode.text);
                html += passedNode.text;
            }
    } else {
        var attlist = '';
        if (passedNode.attributes != null){
            var attnum = passedNode.attributes.length;
            for (var i=0; i<attnum; i++){
                att = passedNode.attributes.item(i);
                if (att.nodeValue != null  && (att.nodeValue != "")){
                    attlist += ' '+att.nodeName+'="'+att.nodeValue+'"';
                }
            }
        }
        if (!isRoot)  html += '<'+passedNode.nodeName+ attlist + '>'
        var children = passedNode.childNodes.length
        for (var j=0; j<children; j++){
            Node=passedNode.childNodes.item(j)
            html += domToStringDiver(Node, false)
        }
        if (!isRoot)  html += "</"+passedNode.nodeName+">\r\n"
    }
    return html;
  }


  //=========== Keep this in sync with version above... ==========
  
  function saveThisPage(theWindow){
      alert(theWindow.document.innerHTML);
      var st = randomParam()
      var html = domWidgetToStringDiver(theWindow.document, true);
      var fin = randomParam()
      alert("msdom.saveThisPage time: "+(fin-st))
      return html;
  }
  
  function domBodyToString(bodyElement){
     return bodyElement.innerHTML;
  }
  
  function domAllToString(document){
      return "<HTML>\n"+documentInnerHTML(document)+"\n</HTML>";
  }                                                 
  
  function documentInnerHTML(document){
      return document.documentElement.innerHTML;
  }
  
  function domWidgetToString(root){
    if ( root == null ) {
        alert("ERROR: [domWidgetToString] document root is null");
        return "";
    }
    //pass true for the entry function, for the parameter isRoot.  This omits the root,
    // which is usually just a container.
    //var start = randomParam();
    var newHTML=domWidgetToStringDiver(root, true);
    //var end = randomParam();
    //alert("domWidgetToString: "+(end-start)+" ms");
    //alert(newHTML);
    //logDebug("<pre>"+newHTML.replace(/</g, "&lt;").replace(/>/g, "&gt;")+"</pre>");
    logDebug("<pre>"+newHTML.replace(/</g, "&lt;")+"</pre>");
    return newHTML;
  }



  function domWidgetToStringDiver(passedNode, isRoot){
    var html = '';
    var nodeName;
    if (passedNode.nodeName != null){
        nodeName = passedNode.nodeName;
        if ( nodeName == '!' ) {
            return html;
        }
        if ( nodeName.charAt(0) == '/' ) {
            return html;
        }
    } else {
        nodeName = 'nullNodeName';
    }
    var pnnt = passedNode.nodeType
    if (pnnt == NODE_TEXT){
      html += passedNode.nodeValue;
    } else if (pnnt == NODE_COMMENT && passedNode.text != undefined){
            if (isIElt6){
                html += "<!--" + passedNode.text + "-->";
            } else {
                html += passedNode.text;
            }
    } else if (nodeName == "SCRIPT" || nodeName == "script") {
        //stupid IE treats SCRIPT specially, and says childNodes.length == 0 and nodeValue is empty.
        var src = passedNode["src"]
        var language = passedNode["language"]
        if (src != null && src != ""){
             html += "<SCRIPT language='"+language+"' src='"+src+"' />";
        } else {
             html += "<SCRIPT language='"+language+"'>\r\n"+passedNode.innerHTML+"\r\n</SCRIPT>";
        }
    } else {
        var attlist = '';
        var skipWidgetChildren = false;
        if (passedNode.attributes != null){
            var atts = passedNode.attributes;
            var attnum = atts.length;
            var att, attNodeValue, attNodeEval, attNodeEvalObj;
            var idx = -1;

            //IE stores attributes in a big-ass array, all possible Attribute NodeType values, whether they are present or not.
            //Keep this loop as tight as possible. It will be run about 80 times for each node.
            for (var i=0; i<attnum; i++){
                att = atts.item(i);
                attNodeValue = att.nodeValue
                attNodeName = att.nodeName
                if (attNodeValue != null  && (attNodeValue != "")){
                    if (attNodeName != 'contentEditable'){
                        attNodeEvalObj = passedNode[attNodeName];
                        idx = -1;
                        if ( attNodeEvalObj != null ) {
                            attNodeEval = ""+attNodeEvalObj;
                            idx = attNodeEval.indexOf("function anonymous");
                            if ( idx == 0 ) {
                                attlist += ' '+attNodeName+'="'+attNodeValue+'"';
                            } else {
                                attlist += ' '+attNodeName+'="'+attNodeEval+'"';
                            }
                        } else {
                            attlist += ' '+attNodeName+'="'+attNodeValue+'"';
                        }
                    }
                    if ( attNodeName == 'class' && attNodeValue == 'widget' ) {
                        skipWidgetChildren = true;
                    }
                }
            }
        }
        if (!isRoot)  html += '<'+nodeName+ attlist + '>'
        if (!skipWidgetChildren){
            var theChildNodes = passedNode.childNodes;
            var children = theChildNodes.length
            for (var j=0; j<children; j++){
                Node=theChildNodes.item(j)
                html += domWidgetToStringDiver(Node, false)
            }
        }
        if (!isRoot)  html += "</"+nodeName+">\r\n"
    }
    return html;
  }