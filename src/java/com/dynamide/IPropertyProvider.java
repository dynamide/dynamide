package com.dynamide;

import java.util.Enumeration;

import com.dynamide.datatypes.DatatypeException;

public interface IPropertyProvider {

    public boolean hasProperty(String propertyName);

    public Enumeration getProperties();

    public Enumeration getPropertyNames();

    public String getPropertyStringValue(String name);

    public Object getPropertyValue(String name);

    public void setProperty(String name, String value) throws DatatypeException ;

    public void setProperty(String name, Property value);

    public void removeProperty(String name);

    public Property getProperty(String propertyName);

    public void addProperty(String name, Property value);
}
