package com.dynamide.widgetbeans;

import java.util.Enumeration;
import java.util.Vector;

import com.dynamide.Session;
import com.dynamide.Widget;

import com.dynamide.resource.ResourceManager;

import com.dynamide.util.StringList;

public class TreeBean {
    public TreeBean(){
    }
    public TreeBean(String id, String description, String nodeText, String template){
        m_id = id;
        m_description = description;
        m_nodeText = nodeText;
        m_template = template;
    }

    public static TreeBean createTestTreeOLD(){
        String tmpl = "resources/widgets/com.dynamide.treenode.wm";
        TreeBean root = new TreeBean("rootid", "root", "", tmpl); //this one won't be displayed.
        TreeBean t, t2;
        t = new TreeBean("1.1", "1.1", "", tmpl);
        t.addChild(new TreeBean("1.1.1", "1.1.1", "Foo", tmpl));
        root.addChild(t);
        t = new TreeBean("1.2", "1.2", "Bar", tmpl);
        t.addChild(new TreeBean("1.2.1", "1.2.1", "Mojo", tmpl));
        t.addChild(new TreeBean("1.2.2", "1.2.2", "Hello", tmpl));
        t.addChild(new TreeBean("1.2.3", "1.2.3", "Goodbye", tmpl));
        root.addChild(t);
        return root;
    }

    public static TreeBean createTestTree(){
        if (true) {
            return ResourceManager.getRootResourceManager().createTestTree();
        }



        String tmpl = "resources/widgets/com.dynamide.treenode.wm";
        TreeBean root = new TreeBean("rootid", "root", "", tmpl); //this one won't be displayed.
        TreeBean t, t2;
        t = new TreeBean("1.1", "1.1", "", tmpl);
        t.addChild(new TreeBean("1.1.1", "1.1.1", "Foo", tmpl));
        root.addChild(t);
        t = new TreeBean("1.2", "1.2", "Bar", tmpl);
        t.addChild(new TreeBean("1.2.1", "1.2.1", "Mojo", tmpl));
        t.addChild(new TreeBean("1.2.2", "1.2.2", "Hello", tmpl));
        t.addChild(new TreeBean("1.2.3", "1.2.3", "Goodbye", tmpl));
        root.addChild(t);
        return root;
    }

    private String  m_id = "0";
    public String  getID(){return m_id;}
    public void setID(String  new_value){m_id = new_value;}

    private String  m_description = "";
    public String  getDescription(){return m_description;}
    public void setDescription(String  new_value){m_description = new_value;}

    private String  m_nodeText = "";
    public String  getNodeText(){return m_nodeText;}
    public void setNodeText(String  new_value){m_nodeText = new_value;}

    private String  m_template = "";
    public String  getTemplate(){return m_template;}
    public void setTemplate(String  new_value){m_template = new_value;}


    public String dive(String nodeID, Widget widget, Session session)
    throws com.dynamide.DynamideUncaughtException {
        StringList variables = new StringList();
        variables.addObject("widget", widget);
        variables.addObject("tree", this);
        String expandedHtmlText = "";
        String htmlText = "";
        try {
            htmlText = session.getAppFileContent(m_template);
        } catch (Exception e){
            return "ERROR: [46] couldn't find file: "+m_template;
        }
        expandedHtmlText = session.expandTemplate(variables, htmlText, nodeID);
        return expandedHtmlText;
    }

    private Vector m_children = new Vector();

    public void addChild(TreeBean tb){
        m_children.addElement(tb);
    }

    public Enumeration getChildren(){
        return m_children.elements();
    }

    public int getChildCount(){
        return m_children.size();
    }

}
