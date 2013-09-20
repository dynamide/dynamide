/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

 public class DynamideUncaughtException extends Error{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
/** @param category is a code from com.dynamide.event.ErrorHandlerInfo.EC_*
    */
   public DynamideUncaughtException(String extra, int category, Throwable t){
        super(t);
        m_extra = extra;
        m_category = category;
   }
   public DynamideUncaughtException(String message){
        super(message);
   }
   private String  m_extra = "";
   public String  getExtra(){return m_extra;}
   public void setExtra(String  new_value){m_extra = new_value;}

   private int m_category = com.dynamide.event.ErrorHandlerInfo.EC_SYSTEM;
   public int getCategory(){return m_category;}
   public void setCategory(int new_value){m_category = new_value;}

   public String toString(){
        String m = "";
        try {
            m = getMessage();
            m += getCause() == null ? "" : " cause: "+getCause().getMessage();  // %% Java 1.4 only %%  except that I added it to DynamideException 9/22/2003 10:35PM
        } catch (Throwable t){
            m = "ERR:"+t.getClass().getName()+" message: "+t.getMessage()+" Throwable.toString(): "+t;
        }
        m += " extra: "+m_extra;
        //System.out.println("DynamideUncaughtException to string: "+m);
        return m;
   }

}

