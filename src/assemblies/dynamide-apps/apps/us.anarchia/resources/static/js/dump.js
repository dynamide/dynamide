
    /**
     * Function : dump()
     * Arguments: The data - array,hash(associative array),object
     *    The level - OPTIONAL
     * Returns  : The textual representation of the array.
     * This function was inspired by the print_r function of PHP.
     * This will accept some data as the argument and return a
     * text that will be a more readable version of the
     * array/hash/object that is given.
     * Docs: http://www.openjs.com/scripts/others/dump_function_php_print_r.php
     */
    function dumpr(arr,level) {
        var dumped_text = "";
        if(!level) {
            level = 0;
        }
        if (level > 10){
            console.log("dumpr level > 10. Exiting");
            return;
        }
        
        //The padding given at the beginning of the line.
        var level_padding = "";
        for(var j=0;j<level+1;j++) level_padding += "    ";
        
        if(typeof(arr) == 'object') { //Array/Hashes/Objects 
            for(var item in arr) {
                var value = arr[item];
                
                if(typeof(value) == 'object') { //If it is an array,
                    dumped_text += level_padding + "'" + item + "' ...\n";
                    dumped_text += dump(value,level+1);
                } else {
                    dumped_text += level_padding + "'" + item + "' => \"" + value + "\"\n";
                }
            }
        } else { //Stings/Chars/Numbers etc.
            dumped_text = "===>"+arr+"<===("+typeof(arr)+")";
        }
        return dumped_text;
    }
    
    function stackTrace(){
        //====stack trace==============
         var e = new Error('dummy');
         var stack = e.stack.replace(/^[^\(]+?[\n$]/gm, '')
              .replace(/^\s+at\s+/gm, '')
              .replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@')
              .split('\n');
         console.log(stack);
    }

    function dump(obj, level){
        if (!obj){
            return;
        }
        if(!level) {
            level = 0;
        }
        level++;
        if (level>2){
          return;
        }
        var arr=[];
        if (obj instanceof $){
            arr.push("type: jquery");
            obj = obj[0];
        } else {
                  
        }
        if (obj) {
            var parentsStr = walkParents(obj);        
              arr.push(parentsStr);
              arr.push(" outerHTML:"+obj.outerHTML);
              //arr.push("\r\n\r\n   innerHTML:"+obj.innerHTML);
              //arr.push("\r\n\r\n   innerText:"+obj.innerText);
        }
        return arr.join("; ");
    }
    
    function walkParents(obj){
          var ancestors = [];
          var tn = '', oid = '';
          if (obj.tagName){tn=obj.tagName;}
          if (obj.id){oid = "#"+obj.id;}
          ancestors.push(tn+oid);
          for (var elm = obj.parentNode; elm; elm = elm.parentNode) {
             ancestors.push(elm.tagName
                            +((elm.id) ? "#"+elm.id : "")
                            +((elm.className)? '.'+elm.className : "") 
                            );
          }
          return ancestors.join(">");
    }
    
    function dumpProperties(obj){
        var arr  = [];
        arr.push("\r\nproperties:");
        for (o in obj){
            if (obj.hasOwnProperty(o)){
                arr.push(o+":"+obj[o]);
                //if (typeof obj[o] == 'object' && !(obj[o] === obj)){
                //    dump(obj[o], level);
               // }
            }
        }
        return arr.join("; ");
    }
  

