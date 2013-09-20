/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import com.dynamide.resource.ResourceManager;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;


public class DynamideLoggedException extends Error{

   /**
	 * 
	 */
   private static final long serialVersionUID = 1L;

   private String errorID = "";
   public String getErrorID() {
		return errorID;
	}

   public DynamideLoggedException(Session session, String message, String extra, Throwable t){
        super(message, t);
        if ( session != null ) {
            this.sessionid = session.getSessionID();
            this.USER = session.getUSER();
        }
        this.extra = extra;
   }

   public DynamideLoggedException(Session session, String message, String extra){
        super(message);
        if ( session != null ) {
            this.sessionid = session.getSessionID();
            this.USER = session.getUSER();
        }
        this.extra = extra;
        //Just log it here, and make sure the file is in the error logs.
        String body = "<hr/><h3>Message</h3><pre>"+StringTools.escape(message)+"</pre>"
                     +"<hr/><h3>Extra</h3>"+extra;
        String id = "unhandled-exception";
        this.errorID = ResourceManager.writeErrorLog(session, id, "", body, Tools.getStackTrace(this), this.getClass().getName());
        String test = ResourceManager.errorIDToHref(this.errorID);
   }

   public DynamideLoggedException(String message){
        super(message);
   }

   private String sessionid;
   public String getSessionID(){
        return sessionid;
   }
   private String USER;
   public String getUSER(){
        return USER;
   }

   private String extra = "";
   public String getExtra(){return extra;}
   public void setExtra(String new_value){extra = new_value;}

   public String toString(){
        return "DynamideLoggedException"+errorID;//return extra;
   }

   public String dump(){
        String m = toString();
        try {
            m = getMessage();
            m += getCause() == null ? "" : " cause: "+getCause().getMessage();  // %% Java 1.4 only %%  except that I added it to DynamideException 9/22/2003 10:35PM
            m += "<hr />"+extra;
        } catch (Throwable t){
            m = "ERROR:[DynamideLoggedException]:"+t.getClass().getName()+" message: "+t.getMessage()+" Throwable.toString(): "+t;
        }
        return m;
   }


}
