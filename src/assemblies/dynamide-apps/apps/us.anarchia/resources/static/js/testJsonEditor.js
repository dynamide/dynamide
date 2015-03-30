function testJsonEditor(){
    test('{"a":"b",}', '{"a":"b"}');  //tests empty row after comma.  (Invalid JSON, but valid keyboard input.)
    
    test("{a:b,c:d}", '{"a":"b","c":"d"}');
    
    test("{a:b,c:d}", '{"a":"b","c":"d"}');
    
    test('{"a":"b","c":"d"}', '{"a":"b","c":"d"}');
    
    
    test("{a:[b,c]}", '{"a":["b","c"]}');
    
    test("[b,{c:d}]", '["b",{"c":"d"}]');
    
    test("{a:b,o:{A:B},arr:[1,2,3]}",
         '{"a":"b","o":{"A":"B"},"arr":[1,2,3]}');
    
    test('{"a":"b","o":{"A":"B"},"arr":[1,2,3]}',
         '{"a":"b","o":{"A":"B"},"arr":[1,2,3]}');
    
    test("{how:can,i:be,sure:{when:you},don't:[give,me,love],you:give me pale shelter.}",
         '{"how":"can","i":"be","sure":{"when":"you"},"don\'t":["give","me","love"],"you":"give me pale shelter."}');
    clear();
    
    function test(jsonStr, jsonExpected){ 
        var inObj, outObj, inStr, outStr, result;
        
        gSkipPreKeyHook = true;
        clear();
        populate(jsonStr);
        result = saveToJson();
        //console.log("saveToJson result: "+result);
        
        inObj = JSON.parse(jsonExpected);
        inStr = JSON.stringify(inObj);
        try {
            outObj = JSON.parse(result);
        } catch (e){
            console.log("ERROR parsing json result: "+e);
            //stackTrace();
            gSkipPreKeyHook = false;
            return "FAILED";
        }
        outStr = JSON.stringify(outObj);
        if (inStr != outStr){
            console.log("FAILED: strings were not identical. "
                        +"\r\nin:       ==>"+jsonStr+"<=="
                        +"\r\nexpected: ==>"+inStr+"<=="
                        +"\r\nout:      ==>"+outStr+"<==");
            gSkipPreKeyHook = false;
            return "FAILED";
        } else {
            console.log("PASSED: "+jsonStr+"\r\n     ==>"+outStr);
            gSkipPreKeyHook = false;
            return "PASSED";
        }
    }
}
