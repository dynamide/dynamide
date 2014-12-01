package com.dynamide.event;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dynamide.DynamideObject;
import com.dynamide.HandlerResult;
import com.dynamide.Page;
import com.dynamide.Session;
import com.dynamide.util.ServletTools;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

import java.io.BufferedReader;

/** In order to make this class as easy to use as possible from multiple scripting languages
 *  supported by events, the important fields are available as public fields and as bean properties
 *  with proper getters and setters.  That is, foo, getFoo() and setFoo() are all public.
 */
public class ScriptEvent extends DynamideObject implements java.io.Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ScriptEvent(){
        super(null);
    }

    public ScriptEvent(DynamideObject owner){
        super(owner);
        if (owner instanceof Session){
            setSession((Session)owner);
        }
    }

    public void finalize() throws Throwable {
        setSession(null);
        super.finalize();
        //System.out.println("ScriptEvent.finalize");
    }


    /** This is the default value, which means dynamide will continue with the default loading of
     *  pages according to other page order rules, such as calling onQueryNextPage.
     */
    public static final int RA_DEFAULT = 0;

    // %% TODO: add error capability to Widget
    //%% TODO: add Page.setErrorMessage, Page.clearErrorMessage, Session.setErrorMessage (??)
    //         and maybe page or session clearAllErrors.
    /** Use this value for resultAction to re-show the current page, but with errors flagged.
     *  You can set field errors with Field.setError(boolean) and Field.setErrorMessage(String).
     *  If you set any errors, the page will automatically be reshown.   Clear them to move on,
     *  using Widget.clearErrorMessage() or Widget.setErrorMessage(null).
     *
     */
    public static final int RA_SHOW_ERRORS = 1;

    /** Use this value for resultAction to jump directly to the page named by nextPageID.
     */
    public static final int RA_JUMP_TO_PAGE = 2;

    /** If your event generates html/xml and you wish to return exactly the source you generate,
     *  set the resultAction to this value, and set the resultSrc to the source.
     */
    public static final int RA_RETURN_SOURCE = 3;

    /** If your event would like to throw an exception to be caught by the error page, you should
     *  set resultAction to RA_THROW_EXCEPTION, and set the outputObject to the exception, otherwise
     *  dynamide catches all ScriptEvent exceptions, and handles them a little differently, though
     *  in most cases you'll end up with the error page anyway.
     */
    public static final int RA_THROW_EXCEPTION = 4;

    /** Use this value for resultAction to send the client's web browser to a URL.  This is useful
     *  for clearing the POST parameters from the client, so that they don't get re-submitted when
     * the client does a refresh in the browser (e.g. Ctrl-R).
     */
    public static final int RA_SEND_REDIRECT = 5;

    public int resultAction = RA_DEFAULT;
    public int getResultAction(){return resultAction;}
    public void setResultAction(int new_value){resultAction = new_value;}

    public String printResultAction(){
        switch ( resultAction ) {
            case RA_DEFAULT : return "RA_DEFAULT";
            case RA_JUMP_TO_PAGE : return "RA_JUMP_TO_PAGE";
            case RA_SHOW_ERRORS : return "RA_SHOW_ERRORS";
            case RA_RETURN_SOURCE : return "RA_RETURN_SOURCE";
            default: return "UNDEFINED";
        }
    }

    public boolean prettyPrint = true;

    private int responseCode = 0;
    public int getResponseCode(){
        return responseCode;
    }
    public void setResponseCode(int code){
        responseCode = code;
    }

    public String redirectURL = "";

    /** Send the client a redirect, rather than a rendered page, to avoid POST data being re-posted on a refresh.
      */
    public void sendRedirect(String url){
        redirectURL = url;
        resultAction = RA_SEND_REDIRECT;
        session.sendRedirect(url);
        // %%%%%%%%%%%% this line above causes the session to be busy when a redirect from / to /dynamide/doco happens.
        //NO: let the session do it:
        //session.getHandlerResult().redirectURL = url;
    }

    /** Send the client a redirect, to the named page, using the current SESSIONID and applicationPath.
     */
    public void sendRedirectToPage(String pageID){
        sendRedirectToPage(pageID, "", "");
    }

    /** Send the client a redirect, to the named page,
     * using the current SESSIONID and applicationPath, and the supplied action parameter.
     * <pre>
     * sendRedirectToPage("page1", "display");
     * </pre>
     *
     * @param theAction contains a single string value that is the action to be sent to the
     * application_onAction or {pageID}_onAction event.
     */
    public void sendRedirectToPage(String pageID, String theAction){
        sendRedirectToPage(pageID, theAction, "");
    }

    /** Send the client a redirect, to the named page,
     * using the current SESSIONID and applicationPath,
     * and the supplied action parameter,
     * but allowing additional parameters to be sent.
     *
     * @param action contains a single string value that is the action to be sent to the
     * application_onAction or {pageID}_onAction event.
     *
     * @param moreParams contains name-value pairs URL encoded.  Here are some examples:
     * <pre>
     * sendRedirectToPage("page1", "display", "category=sales");
     * sendRedirectToPage("page1", "display", "topic=My+Preferences");
     * </pre>
     * Note that you typically do not send additional parameters, since these would best be set in the
     * Session as Fields before leaving the current event.  If you need to send a redirect to another web site
     * or application, use sendRedirect(String url).
     */
    public void sendRedirectToPage(String pageID, String action, String moreParams){
        resultAction = RA_SEND_REDIRECT;
        //%% Fix this: should let the session do it:
        String more = (action.length()>0) ? "action="+action : "";
        more =  ( more.length() > 0 )
                            ?  ((moreParams.length() > 0) ? more+'&'+moreParams : more)
                            :  moreParams;
        String andMore = more.length()>0 ? "&"+more : "";
        redirectURL = session.getAppURL("next="+pageID+andMore);
        session.sendRedirect(redirectURL);
        //getHandlerResult().redirectURL = redirectURL;
    }

    /** Sets the event resultAction to ..., and sets the resultSrc property, but the
     *  action will occur when the event returns.  @return an empty String, just so you
     *  can follow this style in events if you like:
     *  <pre>
     *   if (condition){
     *      return event.jumpToPage("page1");
     *   }
     *  </pre>
     *  Rather than:
     *  <pre>
     *   if (condition){
     *      event.jumpToPage("page1");
     *      return;
     *   }
     *  </pre>
     *  Both styles will work, and are exactly equivalent, but the first is easier to read.
     */
    public void jumpToPage(String nextPageID){
        this.nextPageID = nextPageID;
        resultAction = RA_JUMP_TO_PAGE;
    }

    /** Sets the event resultAction to RA_RETURN_SOURCE, and sets the resultSrc property,
     *  and the mimeType, but the
     *  action will occur when the event returns.
     */
    public void returnSource(String src, boolean prettyPrint, String mimeType){
        returnSource(src);
        this.prettyPrint = prettyPrint;
        this.mimeType = mimeType;
    }


    /** Sets the event resultAction to RA_RETURN_SOURCE, and sets the resultSrc property, but the
     *  action will occur when the event returns.
     */
    public void returnSource(String src, boolean prettyPrint){
        returnSource(src);
        this.prettyPrint = prettyPrint;
    }

    public void returnSource(String src){
        resultAction = RA_RETURN_SOURCE;
        this.resultSrc = src;
    }

    /** Sets the event resultCode RC_ERROR, the
     *  action will occur when the event returns.
     *  @param t can be null.
     */
    public void returnError(String error, Throwable t){
        resultCode = RC_ERROR;
        evalErrorMsg = error;
        if ( t != null ) {
            this.throwable = t;
            println("returnError called with message: '"+error+"' and error: "+Tools.errorToString(t, true));
        } else  {
            println("returnError called with message: '"+error+"'");
        }

    }

    /** Sets all results from the ScriptEvent passed in, so that you can chain events.
     *  Does not copy references to inputObject or outputObject.
     *  Example usage:
     * <pre>
     *  ScriptEvent res = event.setResultsFrom(widget.fireEvent(inputObject, "onClick");
     * </pre>
     */
    public ScriptEvent setResultsFrom(ScriptEvent other){
        if (other == null) return this;
        this.resultSrc = other.resultSrc;
        this.mimeType = other.mimeType;
        this.resultAction = other.resultAction;
        this.resultCode = other.resultCode;
        this.errorLineNumber = other.errorLineNumber;
        this.evalErrorMsg = other.evalErrorMsg;
        this.redirectURL = other.redirectURL;
        this.prettyPrint = other.prettyPrint;
        this.nextPageID = other.nextPageID;
        this.outputObject = other.outputObject;
        return this;
    }

    public transient Session session;
    public Session getSession(){return session;}
    public void setSession(Session new_value){session = new_value;}

    public transient HttpServletRequest request;
    public HttpServletRequest getRequest(){return request;}
    public void setRequest(HttpServletRequest new_value){request = new_value;}

    public transient HttpServletResponse response;
    public HttpServletResponse getResponse(){return response;}
    public void setResponse(HttpServletResponse new_value){response = new_value;}

    public HandlerResult getHandlerResult(){return getSession().getHandlerResult();}

    public transient Object sender;
    /** If this event is triggered from a specific object, that object will be the sender, e.g. Widget */
    public Object getSender(){return sender;}
    public void setSender(Object new_value){sender = new_value;}

    public transient Page currentPage;
    public Page getCurrentPage(){return currentPage;}
    public void setCurrentPage(Page new_value){currentPage = new_value;}

    public String action = "";
    public String getDMAction(){return action;}
    public void setDMAction(String new_value){action = new_value;}

    public String currentPageID = "";
    public String  getCurrentPageID(){return currentPageID;}
    public void setCurrentPageID(String  new_value){currentPageID = new_value;}

    public String nextPageID = "";
    public String  getNextPageID(){return nextPageID;}
    public void setNextPageID(String  new_value){nextPageID = new_value;}

    public String resultSrc = "";
    public String  getResultSrc(){return resultSrc;}
    public void setResultSrc(String  new_value){resultSrc = new_value;}

    public String mimeType = "";
    public String getMimeType(){
        return mimeType;
    }
    public void setMimeType(String type){
        mimeType = type;
    }

    public transient Object inputObject = null;
    public Object getInputObject(){return inputObject;}
    public void setInputObject(Object new_value){inputObject = new_value;}

    public transient Object outputObject = null;
    public Object getOutputObject(){return outputObject;}
    public void setOutputObject(Object new_value){outputObject = new_value;}

    public int errorLineNumber = -1;
    public int getErrorLineNumber(){return errorLineNumber;}
    public void setErrorLineNumber(int new_value){errorLineNumber = new_value;}

    /** This is informational only. */
    public String eventName = "";
    /** This is informational only. */
    public String getEventName(){return eventName;}
    public void setEventName(String new_value){eventName = new_value;}

    public String getRequestBody() throws Exception {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = request.getReader();
        try {
            char[] charBuffer = new char[1024];
            int bytesRead;
            while ( (bytesRead = reader.read(charBuffer)) != -1 ) {
                sb.append(charBuffer, 0, bytesRead);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    /** Don't set this directly, it will get overwritten by the returned Object of the event.
     */
    //public transient Object returnObject = new Object();

    /** The Object returned by the event.
     */
    //public Object getReturnObject(){return returnObject;}


    //================ Internals ====================================

    public String evalErrorMsg = "";
    public String getEvalErrorMsg(){
        return evalErrorMsg;
    }

    public Throwable throwable = null;
    public Throwable getThrowable(){
        return throwable;
    }

    /** For internal use by dynamide. */
    public static final int RC_OK = 1;
    /** For internal use by dynamide. */
    public static final int RC_ERROR = 2;
    /** For internal use by dynamide. */
    public static final int RC_NO_EVENT_SOURCE = 3;

    /** For internal use by dynamide, changes to this field in the event will be ignored.
     *  To have the event ignored, return an empty string.
     */
    public int resultCode = RC_OK;
    public int getResultCode(){return resultCode;}

    public String printResultCode(){
        switch ( resultCode ) {
            case RC_OK : return "RC_OK";
            case RC_ERROR : return "RC_ERROR";
            case RC_NO_EVENT_SOURCE : return "RC_NO_EVENT_SOURCE";
            default: return "UNDEFINED";
        }
    }


    public String toString(){
        return dump();
    }

    public String dump(){
        return dump("{",
                    "",
                    ": ",
                    ", ",
                    "}",
                    false);
    }

    public static final String DUMPHTML_START = "<table border='1' cellspacing='0' cellpadding='2'>";
    public static final String DUMPHTML_ROWSTART = "\r\n<tr><td>";
    public static final String DUMPHTML_ROWMID = "</td><td>";
    public static final String DUMPHTML_ROWEND = "</td></tr>";
    public static final String DUMPHTML_END = "</table>\r\n";

    public String dumpHTML(){
        return dump(DUMPHTML_START,
                    DUMPHTML_ROWSTART,
                    DUMPHTML_ROWMID,
                    DUMPHTML_ROWEND,
                    DUMPHTML_END,
                    true);
    }


    public String dump(String dstart, String d1, String d2, String d3, String dend, boolean escapeHTML){
        String sOutputObject = (outputObject != null) ? outputObject.toString() : "";
        if ( escapeHTML ) {
            sOutputObject = StringTools.escape(sOutputObject);
        }
        //System.out.println("dump================="+sOutputObject);
        return dstart
          +d1+"resultCode"+d2+printResultCode()+d3
          +d1+"resultAction"+d2+printResultAction()+d3
          +d1+"redirectURL"+d2+redirectURL+d3
          +d1+"prettyPrint"+d2+prettyPrint+d3
          +d1+"resultSrc"+d2+(escapeHTML?"<pre>"+StringTools.escape(resultSrc)+"</pre>":resultSrc)+d3
          +d1+"mimeType"+d2+mimeType+d3
          +d1+"action"+d2+action+d3
          +d1+"currentPageID"+d2+currentPageID+d3
          +d1+"nextPageID"+d2+nextPageID+d3
          +d1+"evalErrorMsg"+d2+evalErrorMsg+d3
          +d1+"currentPage"+d2+currentPage+d3
          +d1+"sender"+d2+sender+d3
          +d1+"session"+d2+session+d3
          +d1+"inputObject"+d2+inputObject+d3
          +d1+"outputObject"+d2+sOutputObject+d3
          //+d1+"returnObject"+d2+returnObject+d3
          +((m_printBuffer == null)
            ? ""
            : d1+"printBuffer"+d2+m_printBuffer.toString()+d3
           )
          +dend;
    }

    public void print(String msg){
        checkPrintBuffer();
        m_printBuffer.append(msg);
        if (session != null){
            session.print(msg);
        } else {
            logDebug("Session is null in ScriptEvent.print. msg: "+msg);
        }
    }

    private StringBuffer m_printBuffer = null;

    private void checkPrintBuffer(){
        if (m_printBuffer == null){
            m_printBuffer = new StringBuffer();
        }
    }
    public void println(String msg){
        checkPrintBuffer();
        m_printBuffer.append(msg);
        m_printBuffer.append("\r\n");
        if (session != null){
            session.println(msg);
        } else {
            logDebug("Session is null in ScriptEvent.print. msg: "+msg);
        }
    }

    public String getQueryParam(String paramName){
        return session.getQueryParam(paramName);
    }

    public boolean hasQueryParam(String paramName){
        return session.hasQueryParam(paramName);
    }

    public String getURL(){
        if ( request!=null ) {
            return ServletTools.getURL(request);
        }
        return "";
    }

}
