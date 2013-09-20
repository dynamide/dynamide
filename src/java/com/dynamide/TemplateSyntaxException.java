package com.dynamide;

public class TemplateSyntaxException extends XMLFormatException {
        /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		public TemplateSyntaxException(String message){
                super(message);
        }
        private String  m_errorHTML = "";
        public String  getErrorHTML(){return m_errorHTML;}
        public void setErrorHTML(String  new_value){m_errorHTML = new_value;}
}

