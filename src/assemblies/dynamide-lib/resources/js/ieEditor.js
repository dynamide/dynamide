  //============= Evil Globals ========================

  var format="HTML"
  var VIEW_HTML = "View HTML"
  var VIEW_TEXT = "View Text"

  //=============== Wacky IE Editor stuff ==============

  // Hide or show the editbar
  function displayToolbar(ed, how) {
    var eb = document.all.editbar
    if (how)
      eb.style.display = "inline"
    else
      eb.style.display = "none"
    if (ed!=null) {
      //eb.style.pixelTop = ed.offsetTop + ed.offsetHeight + 1
      //
      eb.style.pixelLeft = ed.offsetLeft
      //eb._editor = window.frames[ed.id]
      //eb._editor.setFocus()
    }
  }

  // Call the formatting command in the editor
  function doFormat(ed, what) {
    //alert("doFormat: "+dump(ed));
    //debugger       //this line will cause MS Script Debugger to pop up.
    execCommand(ed, what, [2])
  }

  // Call the swapmodes command in the editor
  function swapMode(button, textEdit, textArea) {
    var theFormat = swapModes(textEdit, textArea);
    button.value = theFormat;
  }

  function swapModes(textEdit, textArea) {
    var result=VIEW_HTML; //shouldn't see this value, it gets set below.
    if (format=="HTML") {
        //var foo = textEdit.document.body.innerHTML
        //var res = foo.replace(/</g, "&lt;");
        //    res = res.replace(/>/g, "&gt;");
        //alert(res);
        //alert(foo);
        //textArea.innerHTML = res;
        textEdit.document.body.innerText = textEdit.document.body.innerHTML
        //textEdit.document.body.style.fontFamily = "monospace"
        //textEdit.document.body.style.fontSize = "10pt"
        format="Text"
        result = VIEW_HTML
    } else {
        textEdit.document.body.innerHTML = textEdit.document.body.innerText
        textEdit.document.body.style.fontFamily = ""
        textEdit.document.body.style.fontSize =""
        format="HTML"
        result=VIEW_TEXT
    }
    textEdit.focus()
    var s = textEdit.document.body.createTextRange()
    s.collapse(false)
    s.select()
    return result;
  }


  function execCommand(textEdit, command) {
    if (format=="HTML") {
      var edit = textEdit.document.selection.createRange()
      if (arguments[1]==null)
        edit.execCommand(command)
      else
        edit.execCommand(command,false, arguments[1])
      edit.select()
      textEdit.focus()
    }
  }

