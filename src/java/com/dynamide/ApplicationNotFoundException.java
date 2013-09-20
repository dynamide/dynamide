package com.dynamide;

public class ApplicationNotFoundException extends DynamideException {
   private static final long serialVersionUID = 1L;
   public ApplicationNotFoundException(String uri, String message){
        super(message);
        m_URI = uri;
   }
   public ApplicationNotFoundException(String message){
        super(message);
   }
   public ApplicationNotFoundException(Throwable t){
        super(t);
   }

   private String  m_URI = "";
   public String  getURI(){return m_URI;}
   public void setURI(String  new_value){m_URI = new_value;}

}

