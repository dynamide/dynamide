package com.dynamide.util;

import java.util.*;

import com.dynamide.DynamideObject;

/** <p>A Delphi-style StringList: internally keeps space for an object associated
 *  with every string on the list.  If you don't use it, the object
 *  will just be null.  You can use the object functions or the non-object
 *  functions. For example, add(String) or addObject(String, Object).</p>
 *
 *  <p>The list will behave like a hashtable if you keep the
 *  allowDuplicates property as its default value of 'false'.</p>
 *
 *  <p>There is no public 'sorted' property -- just call sort()
 *  whenever you wish to look at the list sorted.</p>
 */
public class StringList
implements ICompare {
    private class ListItem {
        public ListItem(String str, Object obj){string=str;object=obj;}
        public String string;
        public Object object;
        public String toString(){return string;}
    }

    private String extractString(Object item){
        return ((ListItem)item).string;
    }

    private Object extractObject(Object item){
        return ((ListItem)item).object;
    }

    public StringList(){
        list = new SortVector(this);
    }

    public StringList(int initialSize){
        list = new SortVector(this, initialSize);
    }

    public StringList(Vector v){
        this();
        if ( v == null ) {
            return;
        }
        int v_size = v.size();
        for ( int i = 0; i < v_size; i++ ) {
           addObject((String)v.elementAt(i), null);
        }
    }

    public StringList(Enumeration enumObj){
        this();
        if ( enumObj == null ) {
            return;
        }
        while ( enumObj.hasMoreElements() ) {
            add(enumObj.nextElement().toString());
        }
    }

    public StringList(Collection col){
        this();
        if ( col == null ) {
            return;
        }
        for ( Iterator it = col.iterator(); it.hasNext(); ) {
            add(it.next().toString());
        }
    }

    public StringList(boolean caseSensitive){
        this();
        setCaseSensitive(caseSensitive);
    }

    private boolean ascendingSort = true;
    public boolean getAscendingSort(){return ascendingSort;}
    public void setAscendingSort(boolean ascending){ascendingSort = ascending;}

    private boolean caseSensitive = true;
    public boolean getCaseSensitive(){return caseSensitive;}
    public void setCaseSensitive(boolean caseSensitive){this.caseSensitive = caseSensitive;}

    public String toString(){
        String nl = ";";// "\r\n";
        StringBuffer buff = new StringBuffer(list.size());
        for (Enumeration keys = keys(); keys.hasMoreElements(); ){
             String key = (String)keys.nextElement();
             Object obj = getObject(key);
             buff.append(key +" {obj: "+(obj == null ? "null}" : obj.toString()+'}'));
             buff.append(nl);
        }
        return buff.toString();
    }

    public String dump(){
        String lb = "\r\n\r\n";
        return dump(lb);
    }

    public String dump(String lineBreak){
        return dump("", lineBreak, "; ", "", "", false, false, false);
    }
    public String dump(String lineBreak, boolean sorted){
        return dump("", lineBreak, "; ", "", "", false, sorted, true);
    }

    public static final String DUMPHTML_START = "<table border='1' cellspacing='0' cellpadding='2'>\r\n<tr><td>";
    public static final String DUMPHTML_LINESEP = "</td></tr>\r\n<tr><td>";
    public static final String DUMPHTML_ELEMSEP = "</td><td>";
    public static final String DUMPHTML_OBJPLACEHOLDER = "&#160;";
    public static final String DUMPHTML_END = "</td></tr></table>\r\n";
    public String dumpHTML(){
        return dump(DUMPHTML_START,          //start
                    DUMPHTML_LINESEP,        //lineSeparator
                    DUMPHTML_ELEMSEP,        //elementSeparator
                    DUMPHTML_OBJPLACEHOLDER, //objectPlaceholder
                    DUMPHTML_END,
                    true,
                    true,
                    true);           //end
    }
    public String dumpHTML(boolean sorted, boolean dumpDynamideObjects){
        return dump(DUMPHTML_START,          //start
                    DUMPHTML_LINESEP,        //lineSeparator
                    DUMPHTML_ELEMSEP,        //elementSeparator
                    DUMPHTML_OBJPLACEHOLDER, //objectPlaceholder
                    DUMPHTML_END,
                    true,
                    sorted,
                    dumpDynamideObjects);           //end
    }

    /** See also: com.dynamide.util.Tools.dump(Map.....)
     */
    public String dump(String start, String lineSeparator, String elementSeparator,
                       String objectPlaceholder, String end,
                       boolean dumphtml, boolean sorted, boolean dumpDynamideObjects){
        if (sorted) sort();
        StringBuffer buff = new StringBuffer(start);
        String sd;
        for (Enumeration elems = list.elements(); elems.hasMoreElements(); ){
            Object elem =  elems.nextElement();
            buff.append(extractString(elem));
            Object obj = extractObject(elem) ;
            if (obj!=null){
                buff.append(elementSeparator);
                if ( obj instanceof DynamideObject ) {
                    if (dumphtml && dumpDynamideObjects) {
                        sd = ((DynamideObject)obj).dumpHTML();
                    } else {
                        sd = ((DynamideObject)obj).dump();
                    }
                } else {
                    sd = obj.toString();
                }
                buff.append(sd);
            } else {
                buff.append(elementSeparator);
                buff.append(objectPlaceholder);
            }
            if (elems.hasMoreElements()) {
                buff.append(lineSeparator);
            }
        }
        buff.append(end);
        return buff.toString();
    }

    public String[] toStringArray(){
        return Tools.vectorToStringArray(list);
    }

    public Enumeration elements() {
       return keys();
    }

    /** @return a shallow clone of this StringList as a Map
     */
    public Map getMap(){
       Hashtable result = new Hashtable();
       int list_size  = list.size();
       for (int i=0; i<list_size; i++){
          result.put(extractString(list.elementAt(i)), extractObject(list.elementAt(i)));
       }
       return result;
    }

    public Enumeration keys() {
       return getCopyAsVector().elements();
    }

    public List keysList() {
       return getCopyAsVector();
    }

    public Enumeration objects() {
       return getObjectCopyAsVector().elements();
    }

    public Collection objectsCollection() {
       return getObjectCopyAsVector();
    }

    public Vector getObjectCopyAsVector(){
       Vector result = new Vector();
       int list_size  = list.size();
       for (int i=0; i<list_size; i++){
          result.addElement(extractObject(list.elementAt(i)));
       }
       return result;
    }

    public Vector getCopyAsVector(){
       Vector result = new Vector();
       int list_size  = list.size();
       for (int i=0; i<list_size; i++){
          result.addElement(extractString(list.elementAt(i)));
       }
       return result;
    }

    private SortVector list;

    /** DO NOT mess with the items on the list returned.  If you want to access
     *  individual items, use elements() or keys();
     */
    private Vector getInternalVector(){return list;}

    private boolean allowDuplicates = false;
    public boolean getAllowDuplicates(){return allowDuplicates;}
    public void setAllowDuplicates(boolean new_value){allowDuplicates = new_value;}

    public String getString(int i){
       if (i<0 || i>=list.size())
           return "";
       return extractString(list.elementAt(i));
    }

    public void setString(int i, String newstr, Object obj){
       if (i<0 || i>=list.size())
           throw new IndexOutOfBoundsException();
       list.setElementAt(new ListItem(newstr, obj), i);
    }

    public void setString(int i, String newstr){
       setString(i, newstr, null);
    }

    public String first(){
       if (0==list.size())
           return "";
       return extractString(list.elementAt(0));
    }

    public String last(){
       if (0==list.size())
           return "";
       return extractString(list.elementAt(list.size()-1));
    }

    /** Warning: if the item to be added is already on the list, and allowDuplicates
     *  has been set to false, the index of the existing item is returned.  This does
     *  not mean that the index returned is the list size.  Call size() to get the list size.
     */
    public int add(String newstr){
        return addObject(newstr, null);
    }

    public void addAll(Set set){
        if ( set == null ) {
            return;
        }
        for ( Iterator it = set.iterator(); it.hasNext(); ) {
            add(it.next().toString());
        }
    }

    public int addObject(String newstr, Object obj){
        if ( !allowDuplicates ) {
            int i = indexOf(newstr);  //case sensitive!!
            if ( i > -1 ) {
                return i;
            }
        }
        list.addElement(new ListItem(newstr, obj));
        return list.size() - 1;
    }

    public int put(Object key, Object obj){
        return addObject(key.toString(), obj);
    }

    public Object get(Object key){
        return getObject(key.toString());
    }

    public Object get(String key, boolean ignoreCase){
        return getObject(key, ignoreCase);
    }

    public Object getObject(String key){
        return getObjectAt(indexOf(key));
    }

    public Object getObject(String key, boolean ignoreCase){
        return getObjectAt(indexOf(key, ignoreCase));
    }

    /** @return null if not found or out of bounds
     */
    public Object getObjectAt(int i){
        if (i<0 || i>=list.size())
            return null;
        return extractObject(list.elementAt(i));
    }

    public void setObjectAt(int i, Object obj){
        setString(i, getString(i),obj);
    }

    public void add(Vector other){
        String aString;
        for (Enumeration elems = other.elements(); elems.hasMoreElements(); ){
            aString = extractString(elems.nextElement());
            add(aString);
        }
    }

    public void add(Enumeration elems){
        String aString;
        while (elems.hasMoreElements()){
            aString = extractString(elems.nextElement());
            add(aString);
        }
    }

    public void add(StringList other){
        Vector v = other.getInternalVector();
        int v_size = v.size();
        for (int i = 0; i<v_size; i++){
            Object elem = v.elementAt(i);
            extractString(elem);
            addObject(extractString(elem), extractObject(elem));
        }
    }

    public void clear(){
        //System.out.println("StringList.clear ");Tools.printStackTrace();
        list.removeAllElements();
    }

    public void delete(int index){
        //System.out.println("StringList.delete");Tools.printStackTrace();

        if ( index < list.size()  &  index > -1) {
            list.removeElementAt(index);
        }
    }

    public boolean insertObject(String newstr, Object obj, int index){
        if ( !allowDuplicates ) {
            int i = indexOf(newstr);  //case sensitive!!
            if ( i > -1 ) {
                return false;
            }
        }
        list.insertElementAt(new ListItem(newstr, obj), index);
        return true;
    }

    public boolean insert(String newstr, int index){
        return insertObject(newstr, null, index);
    }

    public int indexOf(String which){
        return indexOf(which, (!getCaseSensitive()));
    }

    public int indexOf(String which, boolean ignoreCase){
        int i = -1;
        String aString;
        boolean matched;
        for (Enumeration elems = list.elements(); elems.hasMoreElements(); ){
            aString = extractString(elems.nextElement());
            i++;
            if (aString != null){
                if (ignoreCase)
                    matched = aString.equalsIgnoreCase(which);
                else
                    matched = aString.equals(which);
                if (matched){
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean containsKey(String key){
        return indexOf(key, false) >= 0;
    }


    public Object remove(String which){
        Object prev = getObject(which);
        remove(which, false);
        return prev;
    }

    /** @return null if not found
     */
    public Object remove(Object which){
        Object obj;
        for (Enumeration elems = list.elements(); elems.hasMoreElements(); ){
            Object elem = elems.nextElement();
            obj = extractObject(elem);
            if ( obj.equals(which) ) {
                remove(extractString(elem));
                return obj;
            }
        }
        return null;
    }

    public void remove(String which, boolean ignoreCase){
        //System.out.println("StringList.remove");Tools.printStackTrace();
        int i = indexOf(which, ignoreCase);
        if ( i > -1 ) {
            list.removeElementAt(i);
        }
    }

    public int size(){
        return list.size();
    }

    public void sort(){
        list.sort();
    }

    public List toSortedList(){
        int sz = size();
        TreeMap tm = new TreeMap();
        for ( int i = 0; i<sz; i++ ) {
            tm.put(getString(i), null);
        }
        Vector v = new Vector(tm.keySet());
        return v;
    }

    public Hashtable toHashtable(){
        Object obj;
        int sz = size();
        Hashtable ht = new Hashtable(sz*2);//hashtables like spare room.
        for ( int i = 0; i<sz; i++ ) {
            obj = getObjectAt(i);
            ht.put(getString(i), obj);
        }
        return ht;
    }

    //================ ICompare interface for quicksort in SortVector ==========

    public boolean lessThan(Object l, Object r) {
        String sl = extractString(l);
        String sr = extractString(r);
        if ( ! caseSensitive ) {
            sl = sl.toLowerCase();
            sr = sr.toLowerCase();
        }
        if (ascendingSort){
            return sl.compareTo(sr) < 0;
        } else {
            return sl.compareTo(sr) > 0;
        }
    }

    public boolean lessThanOrEqual(Object l, Object r) {
        String sl = extractString(l);
        String sr = extractString(r);
        if ( ! caseSensitive ) {
            sl = sl.toLowerCase();
            sr = sr.toLowerCase();
        }
        if (ascendingSort){
            return sl.compareTo(sr) <= 0;
        } else {
            return sl.compareTo(sr) >= 0;
        }
    }

}
