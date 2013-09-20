package com.dynamide.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.dynamide.DynamideObject;

public class Profiler {
    private Profiler(){
        //don't allow direct construction
    }

    public static class SectionInfo{
        long startTime;
        String sectionName;
    }

    public static class MemoryInfo{
        long startTime;
        String classname;
        String objectName;
    }


    private static SectionInfo dummySI = new SectionInfo(); //force a compile
    private static MemoryInfo dummyMI = new MemoryInfo();   //force a compile

    private static Profiler instance = new Profiler();

    /** Returns a VM-shared singleton Profiler
     */
    public static Profiler getSharedProfiler(){
        //System.out.println("=============== getSharedProfiler called =================");//+com.dynamide.util.Tools.getStackTrace());
        return instance;
    }

    /** Factory method for a private profiler -- see also getSharedProfiler()
     */
    public static Profiler createProfiler(){return new Profiler();}

    private static Hashtable g_profilers = new Hashtable();

    public synchronized static Profiler getThreadSharedProfiler(){
        System.out.println("=============== getThreadSharedProfiler called =================");
        String threadname = Thread.currentThread().getName();
        Profiler tp = (Profiler)g_profilers.get(threadname);
        if (tp == null){
            tp = new Profiler();
            g_profilers.put(threadname, tp);
        }
        return tp;
    }

    public synchronized static void releaseThreadSharedProfiler(){
        String threadname = Thread.currentThread().getName();
        g_profilers.remove(threadname);
    }

    public static final char ONE_INDENT = '\t';

    private Vector sectionList = new Vector();
    private Vector output = new Vector();
    private Vector memoryInfos = new Vector();

    public Vector getOutput(){
        return _getOutput();
    }

    public void dump(){
        System.out.println(getOutputString());
    }

    public String getOutputString(){
        return _getOutputString();
    }

    protected String _getOutputString(){
        String result = "";
        for (int i = 0; i < output.size(); i++){
            result += (String)output.elementAt(i) + "\r\n";
        }
        long now = System.currentTimeMillis();
        result += "Output at: " + now + "\r\n";
        return result;
    }

    private Vector _getOutput() {
        long now = System.currentTimeMillis();
        output.addElement("Output at: " + now);
        Vector result = (Vector)output.clone();
        output.removeAllElements();
        return result;
    }

    public void start(){
        _start();
    }

    private void _start(){
        if (sectionList == null)
            sectionList = new Vector();
        long now = System.currentTimeMillis();
        output.addElement("Starting profiler at: " + now);
    }

    public void log(String enterSectionName){
        String tag = getIndent() + ONE_INDENT + "--+ " + enterSectionName + " ["+System.currentTimeMillis()+']';
        output.addElement(tag);
    }


    public void enter(String enterSectionName){
        _enter(enterSectionName);
    }

    private void _enter(String enterSectionName){
        SectionInfo info = new SectionInfo();
        info.startTime = System.currentTimeMillis();
        info.sectionName = enterSectionName;
        sectionList.addElement(info);
        String tag = getIndent() + "-->" + enterSectionName + " ["+info.startTime+']';
        output.addElement(tag);
    }

    public void leave(String leavingSectionName){
        _leave(leavingSectionName);
    }

    private void _leave(String leavingSectionName){
       int count = sectionList.size();
       if (count == 0){
            //System.out.println("Profiler list is empty when leaving: "+leavingSectionName);
            return;
       }
       SectionInfo sectionInfo = (SectionInfo)sectionList.elementAt(count-1);
       String name = sectionInfo.sectionName;
       if (name.equals(leavingSectionName)){
            long atTime = System.currentTimeMillis();
            long diff = atTime - sectionInfo.startTime;
            output.addElement(getIndent() + "<--" + name + '['+atTime+"] time:" + diff+" ms");
            sectionList.removeElementAt(count-1);
       } else {
            String trace = "";
            for (int i=0;i<count;i++){
                trace += ((SectionInfo)sectionList.elementAt(i)).sectionName;
            }
            System.out.println("Mismatched enter() and leave() in profiler.  leaving"
                               + leavingSectionName + " but expected: " + name
                               + "\ntrace:\n" + trace);
       }
    }

    private synchronized void _add(Object o, String name){
        MemoryInfo mi = new MemoryInfo();
        mi.objectName = name;
        //don't hold on to ref: mi.object = o;
        mi.startTime = Tools.now().longValue();
        mi.classname = o.getClass().getName();
        memoryInfos.add(mi);
    }

    public void add(Object o, String name){
        _add(o, name);
    }

    private synchronized void _remove(Object o){
        Iterator it = memoryInfos.iterator();
        while ( it.hasNext() ) {
            MemoryInfo mi = (MemoryInfo)it.next();
            if ( mi.classname == o.getClass().getName() ) {
                memoryInfos.remove(mi);
                return;
            }
        }
    }

    public void remove(Object o){
        _remove(o);
    }

    private synchronized void _clearMemoryMonitor(){
        memoryInfos.clear();
    }

    public void clearMemoryMonitor(){
        _clearMemoryMonitor();
    }

    public void clear(){
        memoryInfos.clear();
        output.clear();
        sectionList.clear();
    }


            //o = mi.object;
            //detail = o.getClass().getName();
            //key = detail;

            /*if (o != null && o instanceof DynamideObject) {
                d= (DynamideObject)o;
                if (d instanceof JDOMFile){
                    //detail = ":"+d.getDotName()+":"+((JDOMFile)d).getFilename();
                } else if (d instanceof ScriptEvent){
                    detail = key + ((ScriptEvent)d).m_stackTrace;
                } else {
                    //detail = ":"+d.getDotName();
                }
            }
            //sl.add(mi.objectName+detail);
            */

    private synchronized void _outputObjects(){
        StringList sl = new StringList();
        Iterator it = memoryInfos.iterator();
        String detail;
        String key;
        Object o;
        DynamideObject d;
        while ( it.hasNext() ) {
            MemoryInfo mi = (MemoryInfo)it.next();
            key =mi.classname;
            String count = "1";
            Object item = sl.getObject(key);
            if ( item != null ) {
                count = ""+(Tools.stringToInt((String)item)+1);
                sl.remove(key);
            }
            sl.addObject(key, count);
        }
        sl.sort();
        Enumeration en = sl.keys();
        int i = 0;
        while ( en.hasMoreElements() ) {
            key = (String)en.nextElement();
            detail = sl.getObjectAt(i).toString();
            System.out.println(key+ " " + detail);
            i++;
        }
    }

    public void outputObjects(){
        _outputObjects();
    }

    private String getIndent(){
        int count = sectionList.size();
        char[] ca = new char [count];
        for (int i = 0; i < count; i++)
            ca[i] = ONE_INDENT;
        return new String(ca);
    }
}
