package com.dynamide.security;

public class User{
    public User(String id)
    throws SecurityException {
        setUserID(id);
    }

    public static final String ANONYMOUS = "anonymous";

    private String m_userID = ANONYMOUS;

    public String  getUserID(){return m_userID;}

    public void setUserID(String  new_value)
    throws SecurityException {
        m_userID = new_value;
    }

}

