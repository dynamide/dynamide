package com.dynamide.util;

import java.util.Iterator;
import java.util.Vector;

import com.dynamide.IValueBean;
import com.dynamide.IValueBeanList;
import com.dynamide.IValueBeanTable;
import com.dynamide.Session;

public class ValueBeanHelper {

    public ValueBeanHelper(Session session, IValueBean bean){
        m_session = session;
        m_bean = bean;
    }

    private Session m_session;
    private IValueBean m_bean;

    public Iterator getColNums(){
        return getColNums(m_bean);
    }

    public Iterator getColNums(IValueBean bean){
        Vector v = new Vector();
        if (m_bean instanceof IValueBeanTable){
            int i=0;
            for ( Iterator it = ((IValueBeanTable)m_bean).getColumnNames(); it.hasNext(); it.next()) {
                v.addElement(""+i);
                i++;
            }
        }
        return v.listIterator();
    }

    public Iterator getDisplayRowNums(){
        Vector v = new Vector();
        if (m_bean instanceof IValueBeanList){
            int iStart = ((IValueBeanList)m_bean).getDisplayFirstRowNum();
            int rc = ((IValueBeanList)m_bean).getDisplayRowCount();
            for (int i=0; i < rc; i++) {
                v.addElement(""+(iStart+i));
            }
        }
        return v.listIterator();
    }


    public Object value(){
        return m_bean.cell();
    }
    public Object cell(){
        if (m_bean instanceof IValueBeanTable){
            return ((IValueBeanTable)m_bean).cell(((IValueBeanTable)m_bean).getCurrentRow(), ((IValueBeanTable)m_bean).getCurrentColumn());
        } else if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).cell(((IValueBeanList)m_bean).getCurrentRow());
        } else {
            return m_bean.cell();
        }
    }

    public Object cell(int r) {
        if (m_bean instanceof IValueBeanTable){
            return ((IValueBeanTable)m_bean).cell(r, ((IValueBeanTable)m_bean).getCurrentColumn());
        } else if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).cell(r);
        } else {
            return m_bean.cell();
        }
    }

    public Object cell(int r, int c)  {
        if (m_bean instanceof IValueBeanTable){
            return ((IValueBeanTable)m_bean).cell(r, c);
        } else if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).cell(r);
        } else {
            return m_bean.cell();
        }
    }

    public Object cell(String r, String c){
        return cell(Tools.stringToInt(r), Tools.stringToInt(c));
    }

    public int getCurrentRow() {//default to 0
        if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).getCurrentRow();
        } else {
            return 0;
        }
    }

    public int getCurrentColumn(){   //default to 0
        if (m_bean instanceof IValueBeanTable){
            return ((IValueBeanTable)m_bean).getCurrentColumn();
        } else {
            return 0;
        }
    }

    public String toString(){
        return m_bean.toString();
    }

    public String getType(int c){
        if (m_bean instanceof IValueBeanTable){
            return ((IValueBeanTable)m_bean).getType(c);
        } else {
            return m_bean.getType();
        }
    }

    public int getDisplayRowCount(){
        if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).getDisplayRowCount();
        } else {
            return 1;
        }
    }

    public int getDisplayFirstRowNum(){
        if (m_bean instanceof IValueBeanList){
            return ((IValueBeanList)m_bean).getDisplayFirstRowNum();
        } else {
            return 0;
        }
    }

    public Iterator getColumnNames(){
        if (m_bean instanceof IValueBeanTable && m_bean != null){
            return ((IValueBeanTable)m_bean).getColumnNames();
        } else {
            return new Vector().listIterator();
        }
    }

}
