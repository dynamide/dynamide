function getNextID(collection, resultFunction){
   function onResult(data) {
        resultFunction(data.nextid);
   }
   getNextID_impl(collection, onResult);
}
function getNextID_impl(collection, resultFunction){   
   // AJAX CALL HERE :
   $.get('/anarchia?qtype=nextid&collection='+collection,resultFunction);
}
function getNextID_intoSelector(collection, resultSelector){
    function resultFunction(data) {
        $(resultSelector).html(data.nextid);
    }
    // AJAX CALL HERE :
   getNextID_impl(collection,resultFunction);
}
function GUID(){
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}



