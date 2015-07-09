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



