package com.dynamide.db;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.dynamide.util.Composite;
import com.dynamide.util.Log;
import com.dynamide.util.Profiler;

import org.webmacro.datatable.DataTable;
import org.webmacro.datatable.SimpleTableRow;

public class TableTree {

    boolean DEBUG = false;

    public TableTree()
    throws Exception {
        root = new Composite("root", null);
        m_preorderList = new Vector();
    }

    public TableTree(DataTable dt, String detailIDColumnName, String detailParentIDColumnName, boolean zeroIsNull)
    throws Exception {
        this();
        if (profile) getProfiler().enter("TableTree constructor");
        Iterator it = dt.iterator();
        while ( it.hasNext() ) {
            SimpleTableRow row = (SimpleTableRow)it.next();
            Object o;
            o = row.get(detailIDColumnName);
            String id = (o != null) ? o.toString().trim() : "";
            o = row.get(detailParentIDColumnName);
            String parentID = (o != null) ? o.toString().trim() : "";
            fixup(zeroIsNull, root, id, parentID, row);
        }
        CompositeToList lister = new CompositeToList(m_preorderList);
        root.walkPreOrder(lister);
        if (profile) getProfiler().leave("TableTree constructor");

    }

    //rules:
    //       The idea here is to create temporaries, in case we find child before parent, then do fix-up.
    //
    //   if parentID empty,
    //     create child, add to root
    //   else
    //     find parent
    //     if parent not found,
    //         create placeholder parent off of root
    //
    //     find child
    //     if found
    //        remap child to parent
    //     else
    //        create child, add to parent
    public void fixup(boolean zeroIsNull, Composite root, String id, String parentID, Object row)
    throws Exception {
        if ( parentID == "" || (zeroIsNull&&parentID.equals("0")) ) {
            Composite child = (Composite)root.findComposite(id);
            if (child==null){
                child = new Composite(id, root);
            }
            child.addAttribute("row", row);
            //root.addChild(child);
            if (DEBUG) System.out.println("new child, no parent. parentID: "+parentID+" id: "+id+" path: "+child.dumpPath()+" row: "+row);
        } else {
            Composite parent = (Composite)root.findComposite(parentID);
            if ( parent == null ) {
                if (DEBUG) System.out.println("new parent off root. parentID: "+parentID+" id: "+id+" row, not using: "+row);
                parent = new Composite(parentID, root);   //placeholder, no row yet.
            }
            Composite child = (Composite)root.findComposite(id);
            if ( child != null ) {
                //fixup:  child was a child of root.  Detach and add to parent, which may be a placeholder.
                if (DEBUG) System.out.println("child not null, fixup. parentID: "+parentID+" id: "+id+" row: "+row);
                child.detach();
                child.setParent(parent);//10/18/2003 1:41AM parent.addChild(child);
                //System.out.println("=== child row: "+child.attributeByName("row"));
                child.addAttribute("row", row);  //added 11/8/2003 9:25PM
            } else {
                if (DEBUG) System.out.println("child null, fixup. parentID: "+parentID+" id: "+id+" row: "+row);
                child = new Composite(id, parent);
                child.addAttribute("row", row);
                //child.setParent(parent);
            }
        }
    }

    private com.dynamide.util.Profiler profiler = null;
    private boolean profile = false; //if you set this to true, init the profiler, else nullify it.
    private Profiler getProfiler(){
        if ( profile && profiler == null ) {
            profiler = com.dynamide.util.Profiler.getSharedProfiler();
        }
        return profiler;
    }

    private Composite root;  //it is possible to have an RDB table with rows that have null parentID,
                             // so there is an automatic root node in this class.  All parents in the rdb table
                             // are children of this root node.  This is like getRootNode() in DOM.
                             // But in general, you won't want to display the root node.

    public Composite getRoot(){
        return root;
    }

    public String dump(){
        return "TreeTable. Tree: \r\n"+root.dump();
    }

    public Composite findComposite(String key)
    throws Exception {
        return (Composite)root.findComposite(key);
    }

    private List m_preorderList = null;
    public Iterator getPreorderIterator(){
        return m_preorderList.iterator();
    }

    public Iterator getPreorderIterator(List exclusions){
        if (profile) getProfiler().enter("TableTree.getPreorderIterator");
        m_preorderList = new Vector();
        CompositeToList lister = new CompositeToList(m_preorderList, exclusions);
        root.walkPreOrder(lister);
        if (profile) getProfiler().leave("TableTree.getPreorderIterator");
        //System.out.println("list: ..............."+Tools.collectionToString(m_preorderList));
        return m_preorderList.iterator();
    }

    public Iterator getPreorderIterator(List exclusions, String includeID){
        Composite node = null;
        try {
            node = (Composite)root.findComposite(includeID);
        } catch (Exception e)  {
            Log.error(TableTree.class, "getPreorderIterator", e);
            node = null;
        }
        if (exclusions != null && node != null && includeID != null && includeID.length()>0 ){
            if ( exclusions.indexOf(includeID)>-1) {
                exclusions.remove(includeID);
            }
            Composite parent  = (Composite)node.getParent();
            String id = "";
            while ( parent != null ) {
                id = parent.getName();
                if ( exclusions.indexOf(id)>-1) {
                    exclusions.remove(id);
                    System.out.println("removing parentid from exclusions: "+id);
                }
                parent = (Composite)parent.getParent();
            }
            /*
            Iterator it = exclusions.iterator();
            while ( it.hasNext() ) {
                String nodeID = (String)it.next();
                System.out.println("testing exclusion: "+nodeID+" includeID: "+includeID);
                if ( nodeID != null && nodeID.length()>0 && node.equals(includeID) ) {
                    exclusions.remove(nodeID);
                }
            }
            */
        }
        return getPreorderIterator(exclusions);
    }

    public class CompositeToList implements com.dynamide.util.ICompositeWalker {
        public CompositeToList(List list){
            m_list = list;
        }
        public CompositeToList(List list, List exclusions){
            m_list = list;
            m_exclusions = exclusions;
        }
        private List m_list;
        private List m_exclusions;
        public List getList(){return m_list;}
        public boolean onNode(com.dynamide.util.IComposite node){
            //System.out.println("onNode: "+node);
            boolean keepDiving = true;
            if (m_exclusions!=null){
               if ( m_exclusions.indexOf(node.getName()) == -1 ) {
                    m_list.add(node);
               } else {
                    m_list.add(node);
                    keepDiving = false;
               }
            } else {
                m_list.add(node);
            }
            return keepDiving;
        }
    }

    /*
    sample tree
    1
       6
       7
    2
    3
       4
       5  label date
          8  label  date
          9  label  date

    Put off sorting siblings for now.  Eventually, you sort not the entire tree
       but the siblings at each level.

       At each level in the tree, each set of siblings are sorted in the parent
        by the sort order (Comparator passed to the parent's collection in the contructor).

        to resort, make a new tree.
        Walk the old tree, creating new nodes in the new tree, and setting the comparator before
        calling the target to insert the siblings.

        composite.postOrderIterator(new SortRule("MODIFIED", new DateComparator());

        public class IntComparator(){

        }

        public class DateComparator(){

        }

        for String, use java.text.Collator

     */

    public static void main(String[]args){

    }
}
