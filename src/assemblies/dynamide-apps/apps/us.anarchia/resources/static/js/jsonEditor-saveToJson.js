   //======================= Save to JSON ======================================

   function clear(){
        var start = $('#SDiv');
        start.empty();
        //setCurrentCell(start[0]);
        resetCurrentCell();
    }
  
    function saveToJson(){
        var start = $('#SDiv');
        var output = cellToJSON(start);
        //console.log("saveToJson output: "+output);
        return output;
    }
   
    function cellToJSON($item) { 
        if ($item.children().length===0){
            var ro = $item.text();
            if (ro.length === 0){
                ro = '""';
            } else if(isNaN(ro)){
                var sq = eq = '"';
                if (ro[0] === '"'){
                    sq = '';
                }
                if (ro.length>0 && (ro[ro.length-1]==='"') && (ro[ro.length-2]!=='\\') ){
                    eq = '';
                }
                ro = sq+ro+eq;
            }
            if (gLogDebugSaveToJson) console.log("ro:"+ro);
            return ro;
        }
        var r = '';
        $item.each(function() {  
            var attr = $(this).attr('class');
            if (this.nodeName==='TABLE'  && $(this).hasClass('objectTABLE') ){
                r += '{'+doObjectTABLE(this)+'}';
            } else if (this.nodeName==='TABLE'  && $(this).hasClass('arrayTABLE') ){
                r += '['+doArrayTABLE(this)+']';
            } else {
                r += cellToJSON($(this).children());
                if (gLogDebugSaveToJson) console.log("cellToJSON:"+r);
            }
        });  
        return r;  
    }  
    
    function doArrayTABLE(item){
        var result = '';
        var count = 0;
        var tbody = $(item).children('TBODY')[0];
        var $trs = $(tbody).children('TR.itemTR');
        if ($trs){
            if (gLogDebugSaveToJson) console.log("$trs.length: "+$trs.length);
            $trs.each(function(){
                if (gLogDebugSaveToJson) console.log("in $trs");
                $this = $(this);
                $tds = $this.children("TD.arrayTD");
                $tds.each(function(){
                    if (this.nodeName==='TD' && $(this).hasClass('arrayTD')){
                        if (count>0) {
                            result += ',';
                        }
                        count++;
                        if (gLogDebugSaveToJson) console.log("before cellToJSON:"+result);
                        result += cellToJSON($this);
                        if (gLogDebugSaveToJson) console.log("cellToJSON in arrayTable:"+result);
                    }
                });
            });
        }
        return result;
    }
    
    function doObjectTABLE(item){
        var result = '';
        var count = 0;
        var tbody = $(item).children('TBODY')[0];
        var $trs = $(tbody).children('TR.itemTR');
        if ($trs){
            $trs.each(function(){
                $this = $(this);
                $nameTD = $($this.children("TD.nameTD")[0]);
                if ($nameTD){
                    //doing name, but only if not empty:
                    var ro = $nameTD.text();
                    if (ro.length>0){   //basically, skip empty rows.
                        if (count>0) {
                            result += ',';
                        }
                        count++;
                        
                        //we are in the name column: always surround with quotes.
                        var sq = eq = '"';
                        if (ro[0] === '"'){
                            sq = '';
                        }
                        if (ro.length>0 && (ro[ro.length-1]==='"') && (ro[ro.length-2]!=='\\') ){
                            eq = '';
                        }
                        ro = sq+ro+eq;
                        result += ro;
                        if (gLogDebugSaveToJson) console.log("result:"+result);
                        
                        //doing value inside this block because name must be present:
                        var $valueTD = $nameTD.next();
                        if ($valueTD[0].nodeName==='TD' && $valueTD.hasClass('valueTD')){
                            result += ':'+cellToJSON($($valueTD[0]));
                            if (gLogDebugSaveToJson) console.log("cellToJSON valueTD:"+result);
                        } else {
                            result += ':""'; 
                            console.log("value was empty for name: "+ro);
                        }
                    } else {
                        console.log("skipping empty row: "+walkParents(this));   
                    }
                }
                
            });
        }
        return result;
    }
    

