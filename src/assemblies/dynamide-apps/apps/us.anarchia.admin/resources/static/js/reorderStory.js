var gPanels = null;
var gPanelsArray = [];
$(document).ready(function(){
        
        //altered duplicate from /Users/vcrocla/src/dynamide/build/resource_root/homes/dynamide/assemblies/com-dynamide-apps-1/apps/us.anarchia/resources/static/panels.html
        function getByStory(storyid){
            // AJAX CALL HERE :
           $.get('/anarchia?qtype=bystory&storyid='+storyid,
              function(data) {
                  console.log('getByStory data: '+JSON.stringify(data));
                  gPanels = data;
                  if (gPanels.length && gPanels.length >-1){
                      var imgs = [];
                      for (var i=0; i<gPanels.length; i++) {
                        imgs.push(gPanels[i].img); 
                      }
                      gPanelIndex = 0;
                      renderTemplate(gPanels);
                      console.log('panels: '+JSON.stringify(gPanels));
                  }
              }
           );
        }
        getByStory('http://anarchia.us/id/stories/2');
        
        $('#showRowsBtn').click(function(){
            gPanelsArray = [];
            var gPanelsArrayIndex = 0;
            $('#storyOrderTable tr').each(function (i, row) {
               var $row = $(row);
               var t = $row.find('.storyIndex').text();
               console.log('storyID: '+t);
               gPanelsArray.push(gPanels[Number(t)]);
            });
            saveStory(gPanelsArray);
            console.log("gPanelsArray: "+JSON.stringify(gPanelsArray));
        });
        
        function saveStory(storyJSON){
            // AJAX CALL HERE :
           $.ajax
            ({
              type: "POST",
              url: "/anarchia/stories",
              dataType: 'json',
              contentType: 'text/plain',    //if you set to json, it sends OPTIONS first.  If you don't set, it tries form encoded, which doesn't appear in event.getRequestBody().
              async: false,
              headers: {
                "Authorization": "Basic " + btoa("laramie" + ":" + "ecotel33")
              },
              data: storyJSON,
              success: function (data){
                console.log('data:'+JSON.stringify(data));
                alert('post complete');
              },
              error: function (data){
                console.log('data:'+JSON.stringify(data));
                alert('post failed');
              }
            });
        }
        
        
    
});
