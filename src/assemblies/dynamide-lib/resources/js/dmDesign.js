//might depend on scripts.js

//var oRow = document.createElement("TR");
//var oTd = document.createElement("<TD><b>hi</b></TD>");
//parent.insertBefore(oRow);
//oRow.insertBefore(oTd);

var lastSelected = null;
var currentTBody = null;
var currentTD = null;

//alert("dmDesign.js loaded");

function dmDesign_TableClicked(passedNode){
   passedNode = event.srcElement;
   //if ( passedNode == null ) {
   //   passedNode = event.srcElement;
   //}

   if (event != null) event.cancelBubble = true;
   if (lastSelected != null){
       lastSelected.className = "";
   }
   if ( opener == null || opener.ideSetCurrentTableWindow == null ) {
      alert("ERROR: dmDesign_TableClicked: opener is not the IDE");
      return;
   }

   //alert("fromElement: "+event.srcElement.nodeName);
   currentTBody = null;
   currentTD = null;
   window.status = "";
   opener.ideSetCurrentTableWindow("");
   opener.ideSetCurrentTableWindow(window);//parent);
   //window.status  = passedNode.nodeName;
   //alert(window.clipboardData.getData("Text"));   // or "URL"
   //alert(dump(passedNode, true));
   if (passedNode != null){
    var parent = passedNode;
    while ( parent != null ) {
       var parentNodeName = parent.nodeName;
       if ( parentNodeName == "#document" ) {
          break;
       }
       window.status = "/"+parentNodeName+window.status;
       if ( parent.nodeName == "TABLE" ) {
         //attributes are always lowercase, elements are uppercase.
         //parent.setAttribute("border", "10");
       }
       //alert("currentTD: "+currentTD + " parentNodeName: "+parentNodeName);
       if ( currentTD == null && parentNodeName == "TD" ) {
           currentTD = parent;
           var parentRow = dmDesign_TableFindParentRow(currentTD);
           //alert("findParentRow: "+parentRow.nodeName+" row index: "+findRowIndex(parentRow));
       }
       if ( parentNodeName == "TBODY" && currentTBody == null ) { //once currentTBody is not null, don't go in here again.
          currentTBody = parent;
          lastSelected = parent;
          lastSelected.className = "selected";
       }
       if ( parentNodeName == "TABLE" && lastSelected != null) { //once currentTBody is not null, don't go in here again.
          lastSelected.className = "selected";

       }
       //keep going, but just to get the path.
       parent = parent.parentNode;
    }
   }
   opener.ideSetCurrentElementPath(window.status);//parent);
}

function dmDesign_GetTable(){
    //alert("dmDesign_GetTable: "+currentTBody);
    if (currentTBody == null){
      return null;
    }
    return currentTBody.parentNode;
}

function dmDesign_GetTD(){
  return currentTD;
}

function dmDesign_GetTR(){
  return dmDesign_TableFindParentRow(currentTD);
}

function dmDesign_TableAddColumnOld(passedTbody){
   if (passedTbody!=null){
      //alert(""+passedTbody.childNodes.length);
      var rows = passedTbody.childNodes;
      var rowslen = rows.length;
      for ( var i = 0; i < rowslen; i++ ) {
         if ( rows[i].nodeName == "TR" ) {
           var row = rows[i];
           var newcell = document.createElement("TD");
           newcell.innerHTML = "me";
           row.appendChild(newcell);
         }
      }
   }
}

function dmDesign_TableInsertTable(rows, columns){
  var res = "<TABLE border='1' cellpadding='0' cellspacing='0'><TBODY>";
  for (var r=0;r<rows;r++){
    res = res + "<TR>";
    for (var c=0;c<columns;c++){
      res = res + "<TD>&nbsp;</TD>";
    }
    res = res + "</TR>";
  }
  res = res + "</TBODY></TABLE>";
  var selection = document.selection.createRange();
  //if (obj.contentEditable == "true"){
  try {
      selection.pasteHTML(res);
      selection.select();
  } catch (selectionError){
      alert("dmDesign_TableInsertTable had an error");
  }
}


/* offset = 0 means add row before current, offset = 1 means add after.
 */
function dmDesign_TableAddRow(offset){
  if ( currentTD != null ) {
    var row = dmDesign_TableFindParentRow(currentTD);
    var index = dmDesign_TableFindRowIndex(row);
    if (row != null){
      var myNewRow = currentTBody.insertRow(index+offset); //parent.insertBefore(oRow);
      var colCount = row.cells.length;
      for ( var c=0; c<colCount; c++ ) {
        var myNewCell = myNewRow.insertCell();
        myNewCell.innerHTML = "&nbsp;";
      }
    } else {
      var myNewRow = currentTBody.insertRow(); //parent.insertBefore(oRow);
    }
  } else if (currentTBody != null){
    var myNewRow = currentTBody.insertRow(); //parent.insertBefore(oRow);
    myNewData = myNewRow.insertCell();
    myNewData.innerHTML = "hello td";
    dmDesign_TableAddColumn(currentTBody);
  }
}

function dmDesign_TableAddColumn(offset){
  if ( currentTD != null ) {
    var tbody = dmDesign_TableFindParentTBody(currentTD);
    var cIndex = dmDesign_TableFindColIndex(currentTD);
    if (cIndex != -1){
      var rows = tbody.rows;
      for ( var r = 0; r<rows.length; r++ ) {
        var row = rows[r];
        var currColCount = row.cells.length;
        var myNewCell = row.insertCell(cIndex+offset);
        myNewCell.innerHTML = "&nbsp;";
      }
    } else {
      window.status = "invalid index from findColIndex";
    }
  } else{
     window.status = "null TD in addColumn";
  }
}

function dmDesign_TableFindParentTBody(td){
  if (td == null){
    return null;
  }
  var tr = dmDesign_TableFindParentRow(td);
  if ( tr != null ) {
    var tbody = tr.parentNode;
    while (tbody != null ) {
        if ( tbody.nodeName == "TBODY" ) {
          return tbody;
        }
        tbody = tbody.parentNode;
    }
  }
  return null;
}

function dmDesign_TableFindParentRow(td){
  if (td == null){
    return null;
  }
  var tr = td.parentNode;
  while (tr != null ) {
      if ( tr.nodeName == "TR" ) {
        return tr;
      }
      tr = tr.parentNode;
  }
  return null;;
}

function dmDesign_TableFindRowIndex(tr){
  if (tr == null){
    return null;
  }
  var foundTbody = null;
  var tbody = tr.parentNode;
  while (tbody != null ) {
      if ( tbody.nodeName == "TBODY" ) {
          foundTbody = tbody;
          break;
      }
      tbody = tbody.parentNode;
  }
  if ( foundTbody != null ) {
    var rows = foundTbody.rows;
    for ( var i = 0; i < rows.length; i++ ) {
      if ( rows[i] == tr ) {
        return i;
      }
    }
  }
  return -1;
}

function dmDesign_TableFindColIndex(td){
  if (td == null){
    return -1;
  }
  var foundTR = null;
  var tr = td.parentNode;
  while (tr != null ) {
      if ( tr.nodeName == "TR" ) {
          foundTR = tr;
          break;
      }
      tr = tr.parentNode;
  }
  if ( foundTR != null ) {
    var cells = foundTR.cells;
    for ( var i = 0; i < cells.length; i++ ) {
      if ( cells[i] == td ) {
        return i;
      }
    }
  }
  return -1;
}