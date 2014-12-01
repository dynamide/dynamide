package com.dynamide;


public class HandlerResult {
    public HandlerResult(String result){
        this.result = result;
        this.expires = 0;
    }
    public HandlerResult(String result, boolean prettyPrint){
        this.result = result;
        this.prettyPrint = prettyPrint;
    }
    public HandlerResult(String result, int expires, boolean prettyPrint){
        this.result = result;
        this.prettyPrint = prettyPrint;
        this.expires = expires;
    }
    public HandlerResult(String result, int expires, boolean prettyPrint, String mimeType){
        this.result = result;
        this.prettyPrint = prettyPrint;
        this.mimeType = mimeType;
        this.expires = expires;
    }
    public HandlerResult(byte[] result, int expires, boolean prettyPrint, String mimeType){
        this.binaryResult = result;
        this.prettyPrint = false;
        this.binary = true;
        this.expires = expires;
        this.mimeType = mimeType;
    }
    public String toString(){
        return result;
    }
    public String dump(){
        return "HandlerResult:{prettyPrint:"+prettyPrint+";binary:"+binary+";mimeType:"+mimeType
               +";notModified:"+notModified+";expires:"+expires+";redirectURL:"+redirectURL+";done:"+done
               +";result.length():"+result.length()+";result:[use toString() to view]}";
    }

    public String result = "";
    public String  getResult(){return result;}
    public void setResult(String  new_value){result = new_value;}

    public byte[] binaryResult = new byte[0];

    public boolean prettyPrint = false;
    public boolean getPrettyPrint(){return prettyPrint;}
    public void setPrettyPrint(boolean new_value){prettyPrint = new_value;}

    public boolean binary = false;
    public boolean getBinary(){return binary;}
    public void setBinary(boolean new_value){binary = new_value;}

    public String mimeType = "text/html";
    public String  getMimeType(){return mimeType;}
    public void setMimeType(String  new_value){mimeType = new_value;}

    public boolean notModified = false;
    public boolean getNotModified(){return notModified;}
    public void setNotModified(boolean new_value){notModified = new_value;}

    public int expires = 0;
    public int getexpires(){return expires;}
    public void setexpires(int new_value){expires = new_value;}

    public String redirectURL = "";
    public String getRedirectURL(){return redirectURL;}
    public void setRedirectURL(String new_value){redirectURL = new_value;}

    public boolean done = false;
    public boolean getDone(){return done;}
    public void setDone(boolean new_value){done = new_value;}

    private int m_responseCode = 0;
    public int getResponseCode(){return m_responseCode;}
    public void setResponseCode(int new_value){m_responseCode = new_value;}

    private String  m_errorMessage = "";
    public String  getErrorMessage(){return m_errorMessage;}
    public void setErrorMessage(String  new_value){m_errorMessage = new_value;}

    public String prettyPrintHTML()
    throws XMLFormatException {
        return DynamideHandler.prettyPrintHTML(result);
    }
}
