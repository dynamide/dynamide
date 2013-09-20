function getQueryStringExcluding(QueryString, excludeArray){
    var separator = "";
    var res = "";
    for (var i=0;i<QueryString.keys.length;i++){
        if (excludeArray[QueryString.keys[i]] == null){
            res += separator + QueryString.keys[i] + '=' + QueryString.values[i];
        }
        separator = '&'; //skip first time, else use it.
    }
    return res;
}



function QueryString(key)
{
    var value = null;
    for (var i=0;i<QueryString.keys.length;i++)
    {
        if (QueryString.keys[i]==key)
        {
            value = QueryString.values[i];
            break;
        }
    }
    return value;
}
QueryString.keys = new Array();
QueryString.values = new Array();

function QueryString_Parse(theLocation)
{
    var query = theLocation.search.substring(1); //get rid of leading ?
    var pairs = query.split("&");

    for (var i=0;i<pairs.length;i++)
    {
        var pos = pairs[i].indexOf('=');
        if (pos >= 0)
        {
            var argname = pairs[i].substring(0,pos);
            var value = pairs[i].substring(pos+1);
            QueryString.keys[QueryString.keys.length] = argname;
            QueryString.values[QueryString.values.length] = value;
        }
    }

}