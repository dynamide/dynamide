package com.dynamide.security;

public class Group {
    public Group(String id)
    throws SecurityException {
        setGroupID(id);
    }

    public static final String ANONYMOUS = "anonymous";

    private String m_groupID = ANONYMOUS;

    public String  getGroupID(){return m_groupID;}

    public void setGroupID(String  new_value)
    throws SecurityException {
        m_groupID = new_value;
    }

}

