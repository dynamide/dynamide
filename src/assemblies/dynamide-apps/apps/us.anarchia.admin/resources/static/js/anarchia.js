function getNextID(collection, resultSelector){
    // AJAX CALL HERE :
   $.get('/anarchia?qtype=nextid&collection='+collection,
      function(data) {
        $(resultSelector).html(data.nextid);
      }
   );
}
getNextID('images', '#nextidDiv');

function GUID(){
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

