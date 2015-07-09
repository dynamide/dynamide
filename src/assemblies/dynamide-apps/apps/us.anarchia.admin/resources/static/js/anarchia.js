function getNextID(collection, resultSelector){
    // AJAX CALL HERE :
   $.get('/anarchia?qtype=nextid&collection='+collection,
      function(data) {
        $(resultSelector).html(data.nextid);
      }
   );
}
getNextID('images', '#nextidDiv');

