package com.dynamide.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dynamide.DynamideHandler;
import com.dynamide.HandlerResult;
import com.dynamide.Page;
import com.dynamide.Persistent;
import com.dynamide.Session;

import com.dynamide.event.ScriptEvent;

import com.dynamide.resource.ResourceManager;

public class Audit{

    /*static class AuditResult{
        public AuditResult(){
        }
        Map passes = new HashMap();
        Map failures = new HashMap();
        public String toString(){
            return passes.toString()+"\r\nFAILURES:"+failures.toString();
        }
    } */

    public static class AuditResult {
        public AuditResult(String name){
            m_name = name;
        }
        private String  m_name = "";
        public String  getName(){return m_name;}
        public void setName(String  new_value){m_name = new_value;}

        private List m_list = new ArrayList();
        public List getList(){return m_list;}

        private int m_failCount = 0;
        public int getFailCount(){return m_failCount;}

        private int m_testCount = 0;
        public int getTestCount(){return m_testCount;}

        public void addPass(AuditItemResult item){
            item.setPassed(true);
            m_list.add(item);
            m_testCount++;
        }
        public void addFail(AuditItemResult item){
            item.setPassed(false);
            m_list.add(item);
            m_testCount++;
            m_failCount++;
        }
    }

    public static class AuditItemResult{
        public AuditItemResult(){
        }
        private boolean passed = false;
        public boolean getPassed(){ return passed;}
        public void setPassed(boolean new_passed){ passed = new_passed;}

        private String applicationPath = "";
        public String getApplicationPath(){ return applicationPath;}
        public void setApplicationPath(String new_applicationPath){ applicationPath = new_applicationPath;}

        private String applicationID = "";
        public String getApplicationID(){return applicationID;}
        public void setApplicationID(String new_value){applicationID = new_value;}

        private String queryParams = "";
        public String getQueryParams(){ return queryParams;}
        public void setQueryParams(String new_queryParams){ queryParams = new_queryParams;}

        private String senderID = "";
        public String getSenderID(){ return senderID;}
        public void setSenderID(String new_senderID){ senderID = new_senderID;}

        private String testID = "";
        public String getTestID(){ return testID;}
        public void setTestID(String new_testID){ testID = new_testID;}

        private String errorMessage = "";
        public String getErrorMessage(){ return errorMessage;}
        public void setErrorMessage(String new_errorMessage){ errorMessage = new_errorMessage;}

        private String resultCodeStr = "";
        public String getResultCodeStr(){ return resultCodeStr;}
        public void setResultCodeStr(String new_resultCodeStr){ resultCodeStr = new_resultCodeStr;}

        public String dump(){
            return "{AuditItemResult::PASSED:"+passed
                   +";senderID:"+senderID
                   +";testID:"+testID
                   +";applicationPath:"+applicationPath
                   +";applicationID:"+applicationID
                   +";queryParams:"+queryParams
                   +";resultCodeStr:"+resultCodeStr
                   +";errorMessage:"+errorMessage
                   +"}";
        }
        public String toString(){
            return "{AuditItemResult::"+(passed?"PASSED":"FAILED")
                   +";"+senderID
                   +";"+testID
                   +";"+applicationPath
                   +";"+applicationID
                   +";"+queryParams
                   +";"+resultCodeStr
                   +";"+errorMessage
                   +"}";
        }
    }

    private static String dressException(Throwable t){
        String r = t.toString();
        try {
            DynamideHandler.prettyPrintHTML("<html><body>"+r+"</body></html>");
            return r;//not the pretty version, just r, but knowing that prettyPrintHTML will succeed later.
        } catch (Exception e){
            Log.debug(Audit.class, "================ Couldn't pretty print audit exception: "+t.getClass().getName());
            return StringTools.escape(r);
        }
    }
    private static void auditSender(AuditResult auditResult,
                                    String applicationPath,
                                    String queryParams,
                                    String senderID,
                                    Persistent sender,
                                    Session session){
        List list = auditResult.getList();
        for (Iterator keys=sender.getEvents().iterator(); keys.hasNext(); ){
            String key = (String)keys.next();
            AuditItemResult ar = new AuditItemResult();
            ar.applicationPath = applicationPath;
            ar.queryParams = queryParams;
            ar.senderID = senderID;
            ar.testID = key;
            ar.applicationID = session.getString("applicationID");
            try {
                ScriptEvent event = session.auditEvent(senderID, key);
                ar.resultCodeStr = event.printResultCode();
                if (event.resultCode == ScriptEvent.RC_OK){
                    auditResult.addPass(ar);
                } else {
                    ar.errorMessage = event.getEvalErrorMsg();
                    auditResult.addFail(ar);
                }
            } catch (Throwable ee){
                ar.errorMessage = dressException(ee);
                auditResult.addFail(ar);
            }
        }
        if (sender instanceof Page){
            AuditItemResult ar = new AuditItemResult();
            ar.applicationPath = applicationPath;
            ar.queryParams = queryParams;
            ar.senderID = senderID;
            ar.testID = "outputPage()";
            try {
                HandlerResult hr = ((Page)sender).outputPage();
                //System.out.println("AUDIT ["+senderID+"] : ----------\r\n"+hr.result+"\r\n------------");

                if (hr.prettyPrint) {
                    ar.testID = "outputPage() [w/prettyPrint]";
                    DynamideHandler.prettyPrintHTML(hr.result);
                }
                auditResult.addPass(ar);
            } catch( Throwable e ){
                ar.errorMessage = dressException(e);
                auditResult.addFail(ar);
            }
        }
    }

    public AuditResult createEmptyAuditResult(String uri, String queryParams){
        return new AuditResult(uri+queryParams);
    }

    public static AuditResult audit(String uri, String queryParams) {
        AuditResult auditResult = new AuditResult(uri+queryParams);
        List list = auditResult.getList();
        Session session = null;
        try {
            try {
            	String debugQueryString = uri + "?" + queryParams;
                session = Session.createSession(uri, debugQueryString, Session.MODE_TEST);

                //First, do session events:
                auditSender(auditResult, uri, queryParams, "application", session, session);

                //Now do all pages and page events in session:
                Iterator pages = session.getPages().iterator();
                while(pages.hasNext()){
                        String pageID = (String)pages.next();
                        try {
                            Page page = session.getPageByID(pageID);
                            if ( page != null && page.getPropertyStringValue("auditable").equalsIgnoreCase("false") ) {
                                continue;
                            }
                            auditSender(auditResult, uri, queryParams, pageID, page, session);
                        } catch (Throwable t2){
                            AuditItemResult ar = new AuditItemResult();
                            ar.applicationPath = uri;
                            ar.queryParams = queryParams;
                            ar.senderID = "application";
                            ar.testID = "load "+pageID;
                            ar.errorMessage = "Couldn't load page '"+pageID+"' : "+dressException(t2);
                            auditResult.addFail(ar);
                        }
                }
            } catch (Throwable t1){
                AuditItemResult ar = new AuditItemResult();
                ar.applicationPath = uri;
                ar.queryParams = queryParams;
                ar.senderID = "application";
                ar.testID = "load()";
                ar.errorMessage = "Couldn't create session: "+dressException(t1);
                auditResult.addFail(ar);
            }
        } finally {
            if (session!=null){
                session.close();
            }
        }
        return auditResult;
    }

    public static void main(String[]args)
    throws Exception{
        ResourceManager.createStandalone();
        System.out.println(audit("/dynamide/doco", "USER=audit").getList().toString()); 
        System.exit(0);
    }

}
