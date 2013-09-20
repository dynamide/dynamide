package com.dynamide.datatypes;

public class ValidationResult {
    public ValidationResult(boolean isValid, String errorMessage){
        m_message = errorMessage;
        m_valid = isValid;
    }
    public ValidationResult(boolean isValid){
        m_valid = isValid;
    }
    private boolean m_valid = true;
    public boolean getValid(){return m_valid;}
    public void setValid(boolean new_value){m_valid = new_value;}

    private String m_message = "";
    public String getMessage(){return m_message;}
    public void setMessage(String new_value){m_message = new_value;}
}

