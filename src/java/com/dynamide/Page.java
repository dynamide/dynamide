/* Copyright (c) 2001, 2002 DYNAMIDE.COM */
package com.dynamide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import com.dynamide.datatypes.DatatypeException;
import com.dynamide.event.ChangeEvent;
import com.dynamide.event.ErrorHandlerInfo;
import com.dynamide.event.IChangeListener;
import com.dynamide.event.IPageChangeListener;
import com.dynamide.event.ScriptEvent;
import com.dynamide.event.ScriptEventSource;
import com.dynamide.util.IComposite;
import com.dynamide.util.RegisteredActions;
import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;
import com.dynamide.util.trace.ExpansionLog;

//import com.dynamide.XMLOutputter;









/** See how the WidgetType class is used for Page default properties, and look at the class javadoc
 *  for Widget.
 *  This class descends from AbstractWidget, as does com.dynamide.Widget.  AbstractWidget
 *  abstracts the code for dealing with the WidgetType. */

// %% CHANGED THE PAGE TO DESCEND FROM WIDGET.  This is to support containers, but I'm not sure it's necessary.
// %% It does mean that a Page can now contain Pages, however.  And containers are really Pages, I think.
public class Page extends Widget implements IChangeListener {

    /* Constructor */
    //package-access only
    /* package access*/ Page(DynamideObject owner, String name, String filename, Session newSession)
     throws JDOMException, IOException, PageLoadException, DatatypeException {
        super(owner, filename, newSession);
        //logDebug("Page initializing from : "+filename);
        super.setID(name);  //set this now so that exceptions can have a decent getDotName, but don't
                            // call setName() yet, since that relies on the type and the properties table.
        setSession(newSession);
        setWantWidgetSpan(newSession.getDesignMode());
        Element propertiesElement = getRootElement().getChild("properties");
        String theType = getTypeFromPropertiesElement(propertiesElement);
        if ( theType == null || theType.trim().length() == 0 ) {
            throw new PageLoadException("No 'type' property found for page in "+filename);
        }
        setType(theType);  //this resets the property table from the default type.
        Persistent.addProperties(this, propertiesElement, getID());   //this is exactly what Widget does in setElement, and it works.
        setName(name);
     }

     //package-access only
     void load()
     throws PageLoadException    {
        loadAllWidgets(false); //new 3/17/2002 2:05PM  Means the application shouldn't necessarily load all pages automatically.
        //but this helps make sure that all widgets are available to the onLoad event.

        fire_onImport();

        fireAllWidgetsLoaded();
        ScriptEvent event = fire_onLoad();
        String name = getName();
        //ScriptEvent.RC_NO_EVENT_SOURCE is not an error, since this is not an mandatory event.
        if ( event.resultCode == ScriptEvent.RC_ERROR ) {
            getSession().logHandlerProc("ERROR", "Script error firing Page onLoad: "+name);
            //System.out.println("[Page constructor, got error from fire_onLoad: "+event);
            throw new PageLoadException (event.evalErrorMsg);
        }
        getSession().logHandlerProc("Page onLoad", "Page '<b>" + name + "</b>' loaded");
    }

    public void finalize() throws Throwable {
        String n = "";
        //try {n = getDotName();} catch (Exception e){}
        //System.out.println("Dynamide Page.finalize: "+getClass().getName() +" ["+ m_id +']');
        cleanup();
        super.finalize();
    }

    public void cleanup(){
        Widget aWidget;
        if ( m_widgets != null ) {
            Enumeration en = m_widgets.objects();
            while (en.hasMoreElements()){
                aWidget = ((Widget)en.nextElement());
                aWidget.cleanup();
            }
        }
        setOwner(null);
        setSession(null);
        if ( m_widgets != null ) {
            m_widgets.clear();
        }
    }

    public final static boolean DEFAULT_WANT_WIDGET_SPAN = true;

    private boolean m_wantWidgetSpan = DEFAULT_WANT_WIDGET_SPAN;
    public boolean getWantWidgetSpan(){return m_wantWidgetSpan;}
    /** For most cases you could call this to eliminate the SPAN tag that surrounds all
     *  widget output; but for the IDE, this must be left using the default value; You can
     * turn it to WantWidgetSpan == false if you will be rendering the page multiple times,
     * for example if you are calling Page.outputPage() inside another page. However, this is
     * done automatically if you call Page.outputContainer() or Page.outputPage(false, true).
     */
    public void setWantWidgetSpan(boolean new_value){m_wantWidgetSpan = new_value;}

    private String getTypeFromPropertiesElement(Element propsEl){
        String aType = "";
        Element typeEl = JDOMFile.findFirstElementWithAttribute(propsEl, "property", "name", "type", false);
        if ( typeEl != null ) {
            Element valueEl = typeEl.getChild("value");
            if ( valueEl != null ) {
                aType = valueEl.getText();
            }
        }
        return aType;
    }


    public String toString(){
        return getClass().getName()+"["+getName()+";"+getObjectID()+"]";
    }

    public String getName(){return m_id;}
    public void setName(String new_value)  throws DatatypeException {
        super.setID(new_value);
        setProperty("name", m_id);
        Property property  = getProperty("name");
        if (property!=null){
            property.set("readOnly", "true");
        }
   }

    private HandlerResult m_handlerResult = null;
    public HandlerResult getHandlerResult(){return m_handlerResult;}
    public void setHandlerResult(HandlerResult new_value){m_handlerResult = new_value;}

    private StringList m_widgets = new StringList();
    /** for webmacro access.*/
    public Enumeration getWidgets(){
        m_widgets.sort();
        return m_widgets.objects();
    }

    public List getWidgetsByInterface(String interfaceName){
        Vector result = new Vector();
        Widget w;
        String ifp;
        int size = m_widgets.size();
        for (int i=0; i < size; i++) {
            w = (Widget)m_widgets.getObjectAt(i);
            ifp = w.getPropertyStringValue("interface");
            if ( ifp.equalsIgnoreCase(interfaceName) ) {
                result.add(w.getID());
            }
        }
        return result;
    }

    public String listWidgets()
    throws Exception {
        String nl = "\r\n";
        String indent  = "    ";
        StringBuffer b = new StringBuffer();
        m_widgets.sort();
        int m_widgets_size = m_widgets.size();
        for (int i=0; i < m_widgets_size; i++) {
            String key = m_widgets.getString(i);
            //not needed if you use prop.dump(): b.append(key);b.append(nl);
            Object obj = m_widgets.getObject(key);
            Widget w = ((Widget)obj);
            b.append(indent);
            b.append(""+w.get("type")+": "+w.getID());
            b.append(nl);
        }
        return b.toString();

    }

    public Enumeration getPageProperties(){
        sortPropertiesTable();
        return getPropertiesTable().objects();
    }


    public Object get(String what)
    throws Exception {
        //Deal with name, id, and title specially, otherwise use the lookup defined in AbstractWidget.
        if (   what.equalsIgnoreCase("name")
            || what.equalsIgnoreCase("id")    ) {
                return m_id;
        } else if (what.equalsIgnoreCase("title")){
            String title = super.get("title").toString();
            String designFlag = getSession().getDesignMode() ? " [DESIGN]" : "";
            if ( title.length() == 0 ) {
                return m_id + designFlag;
            } else {
                return title + designFlag;
            }
        } else {
            //return getPropertyStringValue(what);
            String result = super.get(what).toString(); //%%%%%%%%% arg! why must we return String not Object?
            if ( result.length()>0 ) {
                return result;
            }
            Widget w = getWidgetByID(what);
            if ( w != null ) {
                return w;
            }
            return "";
        }
    }

    public Object find(String what){
        Widget widget = getWidgetByID(what);
        if ( widget != null ) {
            return widget;
        }
        return getSession().findDotted(what);
    }

    private void refreshDOM(){
        try{
            readFromString(getFullXMLSource());
        } catch (Exception e){
            System.out.println("*************** ERROR in refreshDOM *************:"+e);
        }
    }

    /** override of JDOMFile.commitInMemoryChangesToDOM().
     */
    public void commitInMemoryChangesToDOM() throws Exception {
        Property property = new Property(this, "lastSave", Tools.now().toString());
        property.set("readOnly", "true");
        property.set("datatype", "java.util.Date");
        setProperty("lastSave", property);

        Element root = getRootElement();

        //events will be first, since no one removes them, then htmlsrc since setHTMLSource does a remove and add,
        // then properties, then widgets, since that's the order here.

        Element properties = getPropertiesElement();
        cleanProperties(properties);
        root.removeChild("properties");
        root.addContent(properties);

        //only do this if the user requests the action
        // or has an option set somewhere.
        // for now, just disable it and let it be run as a command:
        //removeUnusedWidgets(); //Looks at htmlsrc, and compares references to widget
                                 // to the list in m_widgets, and cleans out m_widgets.

        //root.removeChild("widgets");
        Element widgets = root.getChild("widgets");
        if (widgets == null){
            widgets = new Element("widgets");
            root.addContent(widgets);
        } else {
            widgets.removeContent();
        }


        Widget aWidget;
        Element widgetElement;
        m_widgets.sort();
        Enumeration en = m_widgets.objects();
        while (en.hasMoreElements()){
            aWidget = ((Widget)en.nextElement());
            widgetElement = aWidget.toElement();
            widgets.addContent(widgetElement);
        }
        //Note: event source gets saved directly from Persistent.setEventSource().
        //Widget events are owned by the page.
    }

    public String outputContainer(StringList variables)
    throws DynamideUncaughtException {
        String result = outputPage(variables, true, false);
        return result;
    }

    /** Calls outputPage() and returns the HandlerResult.getResult(), pretty printed if
     *  the page determined that it could be pretty printed (errors and such are usually not
     *  pretty printed) so that the result of this method is a string representing the
     *  exact same page that the DynamideHandler would spit out for the DynamideServlet.
     *  This makes this method the preferred call when doing batch processing or publishing.
     *  If you need the mime type, redirection, or the expires, etc., use outputPage().
     */
    public String outputPageResult()
    throws DynamideException {
        HandlerResult hr = outputPage();
        String content;
        boolean debugTransformations = com.dynamide.util.Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
        if (hr.getPrettyPrint()){
            if (debugTransformations){
                logDebug("Page.outputPageResult before prettyPrintHTML:\r\n-----------------\r\n"+hr.getResult()+"\r\n-----------------");
            }
            content = DynamideHandler.prettyPrintHTML(hr.getResult());
            if (debugTransformations){
                logDebug("Page.outputPageResult after prettyPrintHTML:\r\n-----------------\r\n"+hr.getResult()+"\r\n-----------------");
            }
        } else {
            content = hr.getResult();
        }
        return content;
    }

    /**Outputs the page, but wrapped in a HandlerResult, which tells the caller the
     * mime type, expiration time, redirect, and whether prettyPrint is appropriate.
     *
     * @see #outputPageResult
     */
    public HandlerResult outputPage()
    throws DynamideUncaughtException {
        HandlerResult handlerResult = new HandlerResult("", true);
        return outputPage(handlerResult);
    }

    public HandlerResult outputPage(HandlerResult handlerResult)
    throws DynamideUncaughtException {
        setHandlerResult(handlerResult);
        try {
            if ( checkOnOutput(handlerResult)){
                return handlerResult;
            }
            String res = outputPage(new StringList());
            handlerResult.result = res;
            return handlerResult;
        } finally {
            setHandlerResult(null);
        }
    }

    /**@return true if event has created source to return with  ScriptEvent.RA_RETURN_SOURCE
     */
    protected boolean checkOnOutput(HandlerResult handlerResult){
        ScriptEvent event = fire_onOutput();
        if ((event.resultAction == ScriptEvent.RA_RETURN_SOURCE) && (event.resultSrc.length()>0)){
                getSession().logHandlerProc("INFO", "returning onOutput event.resultSrc");
                handlerResult.result = event.resultSrc;
                handlerResult.prettyPrint = event.prettyPrint;
                getSession().setActivePage("");
                return true;
        }
        return false;
    }

    /** Output the page rendered with the widget tag. All widget tags expand into
     *  full html widget text when called this way.  (This is not the WidgetView factory
     *   -- that kind of rendering in the Swing GUI is totally different.)
     *  @param variables is a set of name-value pairs that will be put into the webmacro context.
     */
    public String outputPage(StringList variables)
    throws DynamideUncaughtException {
        return outputPage(variables, false, DEFAULT_WANT_WIDGET_SPAN);
    }

    public String outputPage(boolean innerBodyOnly, boolean wantWidgetSpan)
    throws DynamideUncaughtException {
        return outputPage(new StringList(), innerBodyOnly, wantWidgetSpan);
    }

    public String outputPage(StringList variables, boolean innerBodyOnly, boolean wantWidgetSpan)
    throws DynamideUncaughtException {
        ScriptEvent event;
        String resultOutput;
        boolean prevWantWidgetSpan = m_wantWidgetSpan;
        setWantWidgetSpan(wantWidgetSpan);
        ExpansionLog eLog = getSession().getExpansionLog();  //will be null if expansion logging is off.
        IComposite expansionLogNode = null;
        try {
            loadAllWidgets();
            try {
                event = fire_beforeOutput();
                if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                    String emsg = "[Page.outputPage] error when firing beforeOutput event:"+event.dumpHTML();
                    Exception ex = new DynamideException(emsg);
                    return getSession().hookException(emsg, ex, ErrorHandlerInfo.EC_PAGE);
                }

                String browserid = getSession().getBrowserStringID();
                Persistent.Source source = getSourceElement(browserid);
                String logname = getDotName()+".htmlsrc["+source.browserID+"]";
                if (source.isWebMacro){
                    Element html = source.element;
                    if ( html == null ) {
                        //===========================================
                        //====10/30/2003 7:27PM
                        String expandedHtmlText = expandPageMacros(source.source, variables, logname);
                        JDOMFile temp = new JDOMFile(this);

                        //may need this:
                        boolean hideEntities = true;
                        if (hideEntities) expandedHtmlText = JDOMFile.hideEntities(expandedHtmlText);
                        //logDebug("Page.outputPage //////////////////////////\r\n"+expandedHtmlText+"\r\n//////////////////////");
                        if (eLog!=null){
                            expansionLogNode
                                = eLog.enterExpansion(getSession(),
                                                      getDotName(),
                                                      "htmlsrc-expanded",
                                                      "text/html",
                                                      ExpansionLog.formatEscapePre(expandedHtmlText),
                                                      "Page.outputPage");
                        }

                        if (source.isXHTML == false){    //added 2012-03-20
                            return expandedHtmlText;    //TODO: this early-return may skip expansion logging.
                        }


                        try {
                            Document doc = temp.readFromString(expandedHtmlText);
                            html = doc.getRootElement();
                            if ( innerBodyOnly ) {
                                html = extractInnerBody(html);
                            }
                        } catch (Exception jde){
                            Session session = getSession();
                            int linenum = Session.getJDOMExceptionLineNum(jde);
                            String errorHTML = Session.formatTemplateSyntaxException(jde.getMessage(), expandedHtmlText, linenum);
                            if (eLog!=null) {
                                eLog.logExpansionError(expansionLogNode, "error", "text/html", errorHTML, "Page.outputPage");
                                throw new DynamideLoggedException(session,
                                                                  "ERROR[Page.outputPage] in outputing "+getDotName(),
                                                                  eLog.printExpansionLog(expansionLogNode));
                            } else {
                                throw new DynamideUncaughtException("ERROR[Page.outputPage] in outputing "+getDotName(),
                                                                    ErrorHandlerInfo.EC_PAGE,
                                                                    jde);
                            }
                        }
                        if (eLog!=null){
                            String htmlOutputDebug = JDOMFile.output(html);
                            eLog.leaveExpansion(expansionLogNode, "result", "text/html", ExpansionLog.formatEscapePre(htmlOutputDebug), "Page.outputPage");
                        }

                        Element html2 = (Element)html.clone();
                        resultOutput = outputPageFromSource(variables, html2, "", false /*source.isWebMacro*/, logname);//this time DON'T call webmacro
                        event = fire_afterOutput();
                        if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                            String emsg = "[Page.outputPage] error when firing afterOutput event:"+event.dumpHTML();
                            Exception ex = new DynamideException(emsg);
                            return getSession().hookException(emsg, ex, ErrorHandlerInfo.EC_PAGE);
                        }
                        return resultOutput;

                        //============================================
                        //10/30/2003 7:35PM replaced with above.
                        //WAS: return getSession().hookException("[Page.outputPage] null result from getSourceElement(\""+browserid+"\")", null, ErrorHandlerInfo.EC_PAGE);
                        //============================================
                    }
                }
                if (source.isXHTML){
                    Element html = source.element;
                    if ( html == null ) {
                        return getSession().hookException("[Page.outputPage] null result from getSourceElement(\""+browserid+"\")", null, ErrorHandlerInfo.EC_PAGE);
                    } else {
                        if ( innerBodyOnly ) {
                            html = extractInnerBody(html);
                        }
                        Element html2 = (Element)html.clone();
                        resultOutput = outputPageFromSource(variables, html2, "", source.isWebMacro, logname);
                        event = fire_afterOutput();
                        if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                            String emsg = "[Page.outputPage] error when firing afterOutput event:"+event.dumpHTML();
                            Exception ex = new DynamideException(emsg);
                            return getSession().hookException(emsg, ex, ErrorHandlerInfo.EC_PAGE);
                        }
                        return resultOutput;
                    }
                } else {
                    //HandlerResult hr2 =
                    resultOutput = outputPageFromSource(variables, null, source.source, source.isWebMacro, logname);
                    //hr2.prettyPrint = false; //because not source.isXHTML
                    event = fire_afterOutput();
                    if ( event.resultCode == ScriptEvent.RC_ERROR ) {
                        String emsg = "[Page.outputPage] error when firing afterOutput event:"+event.dumpHTML();
                        Exception ex = new DynamideException(emsg);
                        return getSession().hookException(emsg, ex, ErrorHandlerInfo.EC_PAGE);
                    }
                    return resultOutput;

                }
            } catch (XMLFormatException e){
                return getSession().hookException("[Page.outputPage] caught XMLFormatException from getSourceElement(getSession().getBrowserStringID())", e, ErrorHandlerInfo.EC_PAGE);
            }
        } finally {
            setWantWidgetSpan(prevWantWidgetSpan);
        }
    }

    private Element extractInnerBody(Element html){
        Element body = html.getChild("body");  //html will be <HTML>.
        if ( body == null ) {
            body = html.getChild("BODY");
        }
        if ( body != null ) {
            Element span = (Element)body.clone();
            span.setName("span"); //just a dummy container to replace whatever 'body' contained.  "head" section is thrown away.
            span.setAttribute("containerDotname", AbstractWidget.widgetTypeToScriptName(getDotName())); //widgetTypeToScriptName just replaces dots with _ and so on.
            html = span;
        }
        return html;
    }

    private String outputPageFromSource(StringList variables, Element current, String source, boolean isWebMacro, String logname)
    throws DynamideUncaughtException {
        try {
            String res;
            if (current != null){
                res = outputPageByElement(variables, current, isWebMacro, logname);
            } else {
                if (source.length()==0){
                    logWarn("Page.outputPage: null parameter 'current' and 'source' was empty.  Is there a valid"
                            +" htmlsrc element in the Page xml file?"
                            +" [page.isWebMacro=='"+isWebMacro+"']");
                    //But allow empty source pages to be produced.
                    res = "";
                } else {
                    if (isWebMacro){
                        res = expandPageMacros(source, variables, logname);
                    } else {
                        res = source;
                    }
                }
            }
         //removed 10/15/2003    clearError();
         //removed 10/15/2003    clearFieldErrors();

            //System.out.println("Page.output: \r\n"+res);
            return res;
        } catch (com.dynamide.DynamideUncaughtException due){
            throw due;
        } catch (com.dynamide.DynamideLoggedException dle){
            throw dle;
        } catch (com.dynamide.TemplateSyntaxException te){
            logError("[5]");
            getSession().logHandlerProc("ERROR", "Returning TemplateSyntaxException");
            return getSession().hookException("Template Error: "+te.getErrorHTML(), te, ErrorHandlerInfo.EC_PAGE);
        } catch (Throwable e) {
            //last ditch, a very serious error.
            String throwMsg = "ERROR: [6] caught in Page.outputPage "+getDotName();
            //logError(throwMsg, e);
            return getSession().hookException(throwMsg, e, null, ErrorHandlerInfo.EC_PAGE, false);
        }
    }

    private String outputPageByElement(StringList variables, Element current, boolean isWebMacro, String logname)
    throws Exception {
        boolean wasExpError = false;
        ExpansionLog eLog = getSession().getExpansionLog();  //will be null if expansion logging is off.
        IComposite expansionLogNode = null;
        if (eLog!=null){
            String sCurrent = JDOMFile.output(current, true);
            expansionLogNode
                = eLog.enterExpansion(getSession(),
                                      logname,
                                      "element",
                                      "text/html",
                                      ExpansionLog.formatEscapePre(sCurrent),
                                      "Page.outputPageByElement");
            eLog.logExpansion(expansionLogNode,
                                      "variables",
                                      "text/html",
                                      variables.dumpHTML(true, false),
                                      "Page.outputPageByElement");
        }

        //logDebug("Page.outputPageByElement: "+JDOMFile.output(current));
        boolean hideEntities = true;
        Element replacedHtml = isWebMacro
           ? expandPageMacros(current, variables, hideEntities, logname)
           : current;
        String res = "";
        XMLWidgetOutputter xmloutputter = new XMLWidgetOutputter();
        xmloutputter.setExpandEmptyElements(true);
        xmloutputter.setSession(this.getSession());
        xmloutputter.setPage(this);
        try {
            Element body = null;
            body = replacedHtml.getChild("body");
            if ( body == null ) {
                body = replacedHtml.getChild("BODY");
            }
            if ( body != null ) {
                body.removeAttribute("id");
                body.setAttribute("id", m_id);
                body.removeAttribute("name");
                body.setAttribute("name", m_id);
                boolean dmode = getSession().getDesignMode();
                body.removeAttribute("CONTENTEDITABLE");
                body.setAttribute("CONTENTEDITABLE", ""+dmode);
                //%% If you wish to let the app programmer override this, you'll either have to leave the
                // function in and call the app programmer's function, or you'll have to plug it in in PRO mode.
                body.removeAttribute("onclick");
                if (dmode){
                    body.setAttribute("onclick", "bodyClicked(this)");
                }
            } else {
                Element frameset = replacedHtml.getChild("frameset");
                if ( frameset == null ) {
                    frameset = replacedHtml.getChild("FRAMESET");
                }
                if ( frameset == null ) {
                    //containers don't have them either:
                    //System.out.println("WARNING: [18] couldn't find body tag for id: '"+m_id+"'");
                    //JDOMFile.output(replacedHtml, System.out);
                } else {
                    //System.out.println("Skipping body attribute changes, since current page is a frameset.");
                }
            }
            ByteArrayOutputStream bos =  new ByteArrayOutputStream();
            PrintStream out = new PrintStream(bos);

            if (eLog!=null) {
                eLog.logExpansion(expansionLogNode,
                                      "replacedElement",
                                      "text/html",
                                      ExpansionLog.formatEscapePre(JDOMFile.output(replacedHtml, true)),
                                      "Page.outputPageByElement");
                eLog.logExpansion(expansionLogNode,
                                      "widgets",
                                      "text/html",
                                      ExpansionLog.formatEscapePre(listWidgets()),
                                      "Page.outputPageByElement");
            }
            try {
                //============== Do the output here, using the Element ====================
                xmloutputter.output(replacedHtml, out);
                //=========================================================================
            } catch (Exception e)  {
                if (eLog != null){
                    eLog.logExpansionError(expansionLogNode,
                                           "error",
                                           "text/html",
                                           ExpansionLog.formatEscapePre("ERROR in xmloutputter.output "+Tools.errorToString(e, true)),
                                           "Page.outputPageByElement");
                    throw new DynamideLoggedException(getSession(),
                                                      "ERROR[Page.outputPage] in outputing "+getDotName(),
                                                      eLog.printExpansionLog(expansionLogNode));
                } else {
                    throw e;
                }
            }


            //10/30/2003 10:41AM do the toString, then reset, then close.   //out.close();
            out.flush();
            res = bos.toString();
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
            } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  "+e);}
            out.close();
            if (eLog!=null) eLog.logExpansion(expansionLogNode,
                                      "temp-result",
                                      "text/html",
                                      ExpansionLog.formatEscapePre(res),
                                      "Page.outputPageByElement");
        } finally {
            xmloutputter.setPage(null);
            xmloutputter.setSession(null);
            xmloutputter = null;
        }

        //System.out.println("Page done in outputPage.");
        //if (getSession().getDesignMode()) System.out.println("~~~~~~~~~~~~ this: "+getName()+"-"+hashCode()+" outputPage. root: "+getRootElement().hashCode());
        res = finalExpansions(hideEntities, res);
        if (eLog!=null) eLog.logExpansion(expansionLogNode,
                                  "result",
                                  "text/html",
                                  ExpansionLog.formatEscapePre(res),
                                  "Page.outputPageByElement");
        return res;
    }

    private Element findOrCreateTitle(Element html){
        Element head = html.getChild("head");
        if ( head == null ) {
            head = html.getChild("HEAD");
            if ( head == null ) {
                head = new Element("head");
                html.addContent(head);
            }
        }
        Element title = head.getChild("title");
        if ( title == null ) {
            title = head.getChild("TITLE");
            if ( title == null ) {
                title = new Element("title");
                head.addContent(title);
            }
        }
        return title;
    }

    public static String finalExpansions(boolean unhideEntities, String res){
        if (unhideEntities) {
            res = JDOMFile.unHideEntities(res);
        }
        res = StringTools.searchAndReplaceAll(res, "&apos;", "'"); // %% Hack until JDom beta 7, where they fix this.
        res = StringTools.dm_nbsp(res);
        return res;
    }

    public String expandTemplate(String text)
    throws DynamideUncaughtException {
        StringList variables = new StringList();
        return expandTemplate(text, variables, getDotName());
    }

    public String expandTemplate(String text, StringList variables)
    throws DynamideUncaughtException {
        return expandTemplate(text, variables, getDotName());
    }

    public String expandTemplate(String text, StringList variables, String dotname)
    throws DynamideUncaughtException {
        variables.addObject("page", this);  //always make sure this is defined.
        variables.addObject("pageID", this.getName());  //always make sure this is defined.
        variables.addObject("parent", null);
        return getSession().expandTemplate(variables, text, dotname);
    }

    public Element expandPageMacros(Element html, StringList variables, boolean hideEntities, String logname) throws Exception{
        //String debugFileFullName = "(NOT SET)";
        String expandedHtmlText = "";
        //ExpansionLog eLog = getSession().getExpansionLog();
        //IComposite expansionLogNode
        //    = eLog.enterExpansion(getSession(),
        //                          getDotName(),
        //                          "htmlsrc-pagemacros",
        //                          "text/html",
        //                          "[Element]",
        //                          "Page.expandPageMacros");
        //try {
            String htmlText = "";
            htmlText = JDOMFile.output(html); //static method in JDOMFile.
        //    eLog.logExpansion(expansionLogNode, "result", "text/html", htmlText, "Page.expandPageMacros");
            expandedHtmlText = expandPageMacros(htmlText, variables, logname);
            JDOMFile temp = new JDOMFile(this);
            if (hideEntities) expandedHtmlText = JDOMFile.hideEntities(expandedHtmlText);
            Document doc = temp.readFromString(expandedHtmlText);
            Element result = doc.getRootElement();
            return result;
        //} catch (org.jdom.JDOMException jde){
        //    eLog.logExpansionError(expansionLogNode, "error", "text/html", jde.getMessage(), "Page.expandPageMacros");
        //    throw new DynamideLoggedException(getSession(),
        //                                      "ERROR[Page.expandPageMacros] in "+getDotName(),
        //                                      eLog.printExpansionLog(expansionLogNode));
        //}
    }

    private String expandPageMacros(String source, StringList variables, String logname)
    throws DynamideUncaughtException {
        //logDebug("expandPageMacros(String source, StringList variables) :: "+source.length());
        boolean debugTransformations = com.dynamide.util.Log.getInstance().isEnabledFor(Constants.LOG_TRANSFORMATIONS_CATEGORY, org.apache.log4j.Priority.DEBUG);
        if (debugTransformations){
            logDebug("Page.expandPageMacros before webmacro expansion:\r\n-----------------\r\n"+source+"\r\n-----------------");
            if (variables != null && variables.size()!=0) {
                logDebug("Page.expandPageMacros variables:\r\n-----------------\r\n"+variables.dump()+"\r\n-----------------");
            }
        }
        String expandedHtmlText = expandTemplate(source, variables, logname);
        //debugFileFullName = "C:\\temp\\"+m_id+".xml";
        //FileTools.saveFile("C:\\temp", m_id+".xml", expandedHtmlText);
        //expandedHtmlText = StringTools.searchAndReplaceAll(expandedHtmlText, "<!-- Attempt ", "-->", "", false, true);
        if (debugTransformations){
            logDebug("Page.expandPageMacros after webmacro expansion:\r\n-----------------\r\n"+expandedHtmlText+"\r\n-----------------");
        }
        return expandedHtmlText;
    }

    /** If the widget is already in the DOM, either because it was in the xml file when the form file was
     *   loaded, or because it was inserted there when the client-side designed page was saved, then this
     *   function is pertinent.  Otherwise, Session.renderWidget should be called.
     */
    public String outputWidget(String widgetID)
    throws Throwable {
        HttpServletRequest request = null;
        if ( getSession() != null ) {
            request = getSession().getRequest();
        }
        return outputWidget(widgetID, request);
    }

    public String outputWidget(String widgetID, HttpServletRequest request)
    throws Throwable {
        try {
            String res = "";
            loadAllWidgets();
            XMLWidgetOutputter outputter = new XMLWidgetOutputter();
            outputter.setExpandEmptyElements(true);
            outputter.setSession(this.getSession());
            outputter.setPage(this);
            try {
                //System.out.println("Page trying to outputWidget: "+getName());
                //6/22/2003 9:55AM Element start = getRootElement().getChild("htmlsrc"); //arg!  %% gets randomly for the browser, in document order!!!%%%%%%%%%%%%%
                Element start = getHtmlsrcElement();
                Element widget = findFirstElementWithAttribute(start, "div", "id", widgetID, true);
                if (widget == null){
                    widget = findFirstElementWithAttribute(start, "span", "id", widgetID, true);
                }
                //System.out.println("widget '"+widgetID+"' in outputWidget: "+widget);
                if ( widget == null ) {
                    //see if it is in the created widgets, but just not in the DOM yet.
                    res = getSession().renderWidget(widgetID, this.getName(), request);
                } else {
                    ByteArrayOutputStream bos =  new ByteArrayOutputStream();
                    PrintStream out = new PrintStream(bos);
                    outputter.output(widget, out);
                    //out.close();
                    out.flush();
                    res = bos.toString();
                    try {
                        if(bos!=null)bos.reset();
                        else System.out.println("bos was null, not closing");
                    } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  "+e);}
                    out.close();
                }
            } finally {
                outputter.setPage(null);
                outputter.setSession(null);
                outputter = null;
            }

            //System.out.println("Page done in outputWidget.");
            return StringTools.searchAndReplaceAll(res, "&apos;", "'"); // %% Hack until JDom beta 7, where they fix this.
             //basically until then, they transform this [ select onclick="alert('hi')" ] to this: [ select onclick="alert(&apos;hi&apos;)" ]
        } catch (Exception e) {
            return getSession().hookWidgetError(this, widgetID, "ERROR [7] in outputWidget on pageID: '"+m_id+"' widgetID: '"+widgetID+"'", e);
        }
    }

    public Map findWidgetsInHTMLSource(String browserStringID){
        Element html = null;
        Persistent.Source source = null;
        try {
            source = getSourceElement(browserStringID);
        } catch (XMLFormatException ex){
            System.out.println("Couldn't find any Elements in htmlsrc, because getSourceElement threw exception: "+ex);
            return null;
        }

        if (source == null || source.element == null){
            System.out.println("Couldn't find any Elements in htmlsrc, because getSourceElement returned null");
            return null;
        }
        html = source.element;
        //System.out.println("output: "+output(html));
        XMLWidgetOutputter xmloutputter = new XMLWidgetOutputter();
        ByteArrayOutputStream bos =  new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos);
        xmloutputter.setMode(XMLWidgetOutputter.FIND_WIDGETS);
        try {
            try {
                //System.out.println("findWidgetsInHTMLSource: "+html);
                xmloutputter.output(html, out);
                StringList widgets = xmloutputter.getFoundWidgets();
                //System.out.println("FOUND WIDGETS on Page["+m_id+"]: "+widgets.dump());
                return widgets.getMap();
            } catch (IOException e){
                System.out.println("Couldn't find any Elements in htmlsrc, because xmloutputter threw exception: "+e);
            }
        } finally {
            try {
                if(bos!=null)bos.reset();
                else System.out.println("bos was null, not closing");
                out.close();
            } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in  Page.findWidgetsInHTMLSource "+e);}
            xmloutputter.setMode(XMLWidgetOutputter.PRINT_WIDGETS);
        }
        return null;
    }

    public void removeUnusedWidgets(){
        Map map = findWidgetsInHTMLSource("*"); //%% arg. do for all browsers???  This just does for default browser.
        removeWidgetsNotInList(map);
    }

    public void removeWidgetsNotInList(Map widgetsInHtmlsrc){
        Enumeration enWidgets = getWidgets();
        while ( enWidgets.hasMoreElements() ) {
            Widget w = (Widget)enWidgets.nextElement();
            String wid = w.getID();
            if ( ! widgetsInHtmlsrc.containsKey(wid)) {
                removeWidget(w, wid);
                //logInfo("Page ["+m_id+"] removing unused widget: "+wid);
            }
        }
    }

    //setHTMLSource(Element) removed after cvs'd version 1.6 (had tag "before-new-createWidget")

    private String findNextNameForType(String type){
        int seq = 1;
        String widgetName = AbstractWidget.widgetTypeToScriptName(type);
        while ( getWidgetByID(widgetName+seq) != null ) {
            seq++;;
        }
        return widgetName+seq;
    }

    //----------interface IChangeListener ------------
    public void propertyChanged(ChangeEvent changeEvent){
        if (true) return;

        ////////////// OBVIATED /////////////////////////////////////


        //logDebug("Page.propertyChanged() called >>>>>>>>>>>>>>>>>");
        try{
            Object sender = changeEvent.sender;
            if ( sender instanceof Widget ) {
                Widget widget = (Widget)sender;
                String oldValue = "";
                if (changeEvent.oldValue != null){
                    oldValue = changeEvent.oldValue.toString();
                }
                String newValue = changeEvent.newValue.toString();
                String propName = changeEvent.fieldName;   //changeEvent.fieldName is the property name.
                Element widgetsElem = getRootElement().getChild("widgets");
                Element theWidgetElem = findFirstElementWithAttribute(widgetsElem, "widget", "id", widget.getID(), false);
                if ( theWidgetElem == null ) {
                    System.out.println("Widget '"+widget.getID()+"' is null in xml page (which means it is not in the DOM, probably because file is not saved yet.)  Page.propertyChanged()");
                    return;
                }
                Element properties = theWidgetElem.getChild("properties");
                Element property = findFirstElementWithAttribute(properties, "property", "name", propName, false);
                if ( property != null ) {
                    property.removeContent();
                    property.setText(newValue);  // %% ARG! Again with the string-only values!
                    //this was good but verbose: logDebug("   >>>>>>>>>>>>>>>>> setting text in existing property ["+widget.getID()+"."+propName+"], value: "+changeEvent.newValue.toString());
                    //System.out.println("property >>>>");
                    //output(property, System.out);
                } else {
                    property = new Element("property");
                    property.removeAttribute("name");
                    property.setAttribute("name", propName);
                    property.addContent(newValue);  // %% ARG! Again with the string-only values!
                    properties.addContent(property);
                }
                notifyPageChangeListeners(propName, oldValue, newValue);
            }
        } catch (Exception e){
            logError("[10] :::::Page.propertyChanged: "+e);
            e.printStackTrace();
        }
    }

    //------------------------------------------------


    /** This one is by id, whereas getWidgetNamesList is by name
     */
    public StringList getWidgetList(){
        StringList sl = new StringList();
        Element elem = getRootElement().getChild("widgets");
        if ( elem != null ) {
            List children = elem.getChildren("widget");
            Iterator i = children.iterator();
            while (i.hasNext()) {
                Element widget = (Element) i.next();
                String id = getAttributeValue(widget, "id");
                if ( id.length() > 0 ) {
                    sl.add(id);
                }
            }
        }
        return sl;
    }

    public StringList getWidgetNamesList(){
        StringList sl = new StringList();
        Element elem = getRootElement().getChild("widgets");
        if ( elem != null ) {
            List children = elem.getChildren("widget");
            Iterator i = children.iterator();
            while (i.hasNext()) {
                Element widget = (Element) i.next();
                Element properties = widget.getChild("properties");
                Element res = findFirstElementWithAttribute(properties, "property", "name", "name", false);
                if ( res !=null ) {
                    sl.add(res.getTextTrim());
                }
            }
        }
        return sl;
    }


    /*  Here are the scenarios for creating widgets:
     *     on loading a page for the first time, loadAllWidgets is called.  Do add to hashtable
     *       this calls createWidgetFromElement(Element)
     *
     *     when rendering a page, create one,  but don't put it in the hashtable.
     *       this calls createWidgetFromElement(Element)
     *
     *     When the application.xml or some event needs to create a widget on the fly, it
     *       calls createWidget(String type) , possibly through session.createWidget(type, targetPageID).
     *     In this case, don't addWidget() or persist the widget.
     *
     *     e.g. Session.printSessionDetail()
     *         ---> widget = createWidget()
     *         ---> widget.setType()
     *         ---> widget.setProperty("name", name)
     *         ---> renderWidget()
     *
     *     Widget()
     *        does nothing.
     *
     *
     *
     *     Widget.setElement
     *       --->  Widget.setType
     *            ---> setProperty("type", type)
     *            ---> updateProperties()    //although:  2/10/2002 11:25AM removed updateProperties in Widget CVS version 1.23
     *                ---> loops through properties and calls new Property()  ARG!!!!
     *
     *    Maybe I need setInstanceElement and setDefaultElement
     *
     */

    //12/20/2001 KLUDGE %%  There should be some way to elegantly determine if there are new widgets, reload, etc.
    private boolean m_allWidgetsLoaded = false;

    protected void loadAllWidgets()
    throws DynamideUncaughtException {
        loadAllWidgets(false);
    }

    protected void loadAllWidgets(boolean call_fireAllWidgetsLoaded)
    throws DynamideUncaughtException {
        //logDebug("Page["+getDotName()+"].loadAllWidgets");
        if ( m_allWidgetsLoaded ) {
            return;
        }
        //System.out.println("====== "+getName()+".loadAllWidgets");
        List widgetsElements = getRootElement().getChildren("widgets");
        for (Iterator it = widgetsElements.iterator();it.hasNext();){
            Element widgets = (Element)it.next();
            widgets = resolveWidgetsElement(widgets);
            loadWidgetsFromElement(widgets);
        }
        if ( call_fireAllWidgetsLoaded ) {
            fireAllWidgetsLoaded();
        }
        m_allWidgetsLoaded = true;
    }

    /** return and Element that contains all "widget" Elements, possibly
     *  resolving an "href" attribute to build all the "widget" Elements
     *  from another resource.
     */
    private Element resolveWidgetsElement(Element elem){
        String href = "";
        try {
            if (elem!=null) {
                Session session = getSession();
                href = elem.getAttributeValue("href");
                if ( href != null) {
                    List widgetList = elem.getChildren("widget");
                    if ( widgetList!= null && widgetList.size() != 0 ) {
                        String msg = ("widgets element in page '"+getID()+"' had both an external filename href attribute and \"widget\" elements.  \"widget\" elements will be ignored.");
                        logWarn(msg);
                        if (session.getTestMode()){
                            throw new Exception(msg);
                        }
                    }
                    String widgetsFilename = session.getAppFilename(href);
                    if ( widgetsFilename == null ) {
                        String msg = ("filename specified in page '"+getID()+"'::widgets::filename was null: "+href);
                        logWarn(msg);
                        if (session.getTestMode()){
                            throw new Exception(msg);
                        }
                    } else {
                        JDOMFile widgetsFile = new JDOMFile(null, widgetsFilename);
                        elem = widgetsFile.getRootElement();  //will be "widgets".
                        logInfo("using href for widgets in page '"+getID()+"': '"+widgetsFilename+"'");
                    }
                }
            }
        } catch (Exception e)  {
            throw new DynamideUncaughtException("Could not load widgets from href "+href, ErrorHandlerInfo.EC_PAGE, e);
        }
        return elem;
    }

    private void loadWidgetsFromElement(Element elem){
        if ( elem != null ) {
            List children = elem.getChildren("widget");
            Iterator i = children.iterator();
            Widget widget;
            while (i.hasNext()) {
                Element widgetElement = (Element) i.next();
                if (widgetElement != null){
                    String id = getAttributeValue(widgetElement, "id");
                    if ( ! m_widgets.containsKey(id) ) {
                        widget = createWidgetFromElement(widgetElement);
                        addWidget(widget);
                    }
                }
            }
        }
    }



    protected void fireAllWidgetsLoaded(){
        //logDebug("Page["+getDotName()+"].fireAllWidgetsLoaded()");
        Iterator it = m_widgets.objectsCollection().iterator();
        while ( it.hasNext() ) {
            Widget widget = (Widget)it.next();
            widget.fire_onLoad();
        }
    }

    // %% I changed this to public, not because it should be public, but because I wanted to mess around with adding Pages
    // %% as widgets from the beanshell eval window.
    public void addWidget(Widget wd){
        addWidget(wd, wd.getID());
    }

    private void addWidget(Widget wd, String wdname){
        int i = m_widgets.put(wdname, wd);
        //logDebug("Page["+getDotName()+"].addWidget: "+wdname+" = "+i);
        wd.setPage(this);
        wd.setSession(this.getSession()); //widget.cleanup() undoes this in removeWidget.
        wd.addChangeListener(this);
    }

    private void removeWidget(Widget wd, String oldID){
        wd.removeChangeListener(this);
        m_widgets.remove(oldID);
        wd.removeChangeListener(this);
        wd.cleanup(); //%% new 9/2/2002 9:48AM
    }

    public void renameWidget(Widget wd, String oldName, String newName){
        m_widgets.remove(oldName);
        wd.removeChangeListener(this);
        addWidget(wd, newName);
    }


    public Widget createWidget(String widgetType)
    throws Exception {
        if ( widgetType.length()==0 ) {
            throw new DynamideUncaughtException("widgetType was empty in Page.createWidget.");
        }
        String id = findNextNameForType(widgetType);
        return createWidget(widgetType, id, true);
    }

    public Widget createWidget(String widgetType, String id, boolean callWidget_fire_OnLoad)
    throws Exception {
        if ( widgetType.length()==0 ) {
            throw new DynamideUncaughtException("widgetType was empty in Page.createWidget. ID was: '"+id+"'");
        }
        Widget wd = new Widget(this/*.getOwner()--1/26/2004*/, this.getSession());
        wd.setSession(this.getSession());
        wd.setDesignMode(this.getSession().getDesignMode());
        wd.setType(widgetType);
        if (Tools.isBlank(id)){
            String basename = wd.getWidgetType().getPropertyDefaultValue("basename");
            if ( basename != null && basename.length()>0 ) {
                id = findNextNameForType(basename);
                logDebug("********** using basename for widget: "+basename);
            }
        }
        wd.setID(id);
        wd.setProperty("name", id);
        wd.setPage(this);
        //int theBrowserID = getSession().getBrowserID();
        //wd.setBrowserID(theBrowserID);
        addWidget(wd, id);
        if (callWidget_fire_OnLoad) wd.fire_onLoad(); //12/30/2003
        return wd;
    }

    /** @return null if error or not found.
     */
    public Widget createWidgetSafe(String widgetType){
        try {
            return createWidget(widgetType);
        } catch (Throwable t){
            return null;
        }
    }


    /** Create a widget, and add to the widgets hashtable. */
    public Widget createWidgetFromElement(Element element, boolean generateID, boolean addWidget){
        Widget widget = createWidgetFromElement(element); //%% generateID....
        if ( widget != null && addWidget ) {
            addWidget(widget);
        }
        return widget;
    }

    private int m_generatedWidgetSeq = 0;

    /** Create a widget, but don't add to hashtable or listen to it. */
    private Widget createWidgetFromElement(Element element){

        try {
            //System.out.println("=====\r\ncreateWidgetFromElement "+JDOMFile.output(element)+"\r\n widgets: "+listWidgets());
            //Tools.printStackTrace();

            String widgetID = element.getAttributeValue("id");
            if ( widgetID == null || widgetID.length()==0 ) {
                widgetID = "generatedWidget_"+(m_generatedWidgetSeq++);
            }
            Widget widget;
            int theBrowserID = getSession().getBrowserID();
            widget = getWidgetByID(widgetID);
            boolean created = false;
            if (widget == null){
                widget = new Widget(getOwner(), getSession());
                created = true;
                //System.out.println("createWidgetFromElement created widget: "+widgetID+" because element did not contain id found in page: "+JDOMFile.output(element)+" widgets: "+listWidgets());
            }
            Field f = widget.getField();
            //if ( f != null ) {
            //    System.out.println("widget field, in Page.createWidgetFromElement: "+f.dump());
            //} else {
            //    System.out.println("widget field was null.");
            //}

            widget.setSession(getSession());
            //widget.setBrowserID(theBrowserID);
            if (widget.getPage()!=this) widget.setPage(this);
            widget.setDesignMode(getSession().getDesignMode());
            try {
                if (created) {
                    String elname = element.getName();
                    if (elname.equals("widget")){
                        //logDebug("INITIALIZING widget from element: "+JDOMFile.output(element, true));
                        widget.initFromElement(element);
                    } else {
                        //logDebug("SKIPPING initFromElement since element is an inline widget reference: "+JDOMFile.output(element, true));
                    }
                    //after setElement, the id is available: there is some fancy lookup of references, so the attributes may be in the ref,
                    // so id is not available until after the element is set.  ==> maybe move to constructor %%
                    widget.setID(widgetID);
                }
            }
            catch( Exception e ){
                logError("couldn't setElement in Page.createWidgetFromElement", e);
            }
            return widget;
        } catch( Exception e2 ){
            logError("Couldn't createWidgetFromElement, exception", e2);
            return null;
            }
    }

    public void printSerializedWidget(Element element, Writer out, int indentLevel)
    throws Exception {
        Widget widget = createWidgetFromElement(element);
        //logDebug("[Page.printSerializedWidget] widget: "+widget.toString()+" getSession(): "+getSession()+" widget.page == "+widget.getPage());
        widget.printSerialized(out, indentLevel, m_wantWidgetSpan);
    }

    /* This is funky.  For now, just persist when you save the file.
     * public String addWidget(String type){
        try{
            String name = findNextNameForType(type);
            Element widgetsElem = getRootElement().getChild("widgets");
            Element content = WidgetType.generateEmptyWidgetFor(type, name);
            widgetsElem.addContent(content);
            widgetsElem.addContent("\r\n");
            refreshDOM();
            System.out.println("Successful in addWidget/refreshDOM ................................");
            notifyPageChangeListeners("newWidget", null, name);
            return name;
        } catch (Exception e){
            System.out.println("ERROR::::::addWidget: "+e);
        }
        return "";
    }
    */


    public Widget getWidget(String id){
        return getWidgetByID(id);
    }

    public Widget getWidgetByID(String id){
        if (id!=null){
            Widget aWidget =  (Widget)m_widgets.get(id);
            if ( aWidget != null ) {
                //aWidget.setSession(getSession()); //%% super kludge introduced by Widget.cleanup(), which I call and it hoses these two references.
                //aWidget.setPage(this);
            }
            return aWidget;
        } else {
            System.out.println("******** getWidgetByID can't deal with null id.");
            return null;
        }
    }

    //This used to set the property value as well as getting the widgetDiv, now the caller should call widget.changeProperty(propertyName, propertyValue) first
    public String getWidgetDiv(String widgetID, HttpServletRequest request)
    throws Throwable {
        try {
            Widget w = getWidgetByID(widgetID);
            if ( w == null ) {
                String msg = "ERROR: [11] getWidgetDiv(): widget '"+widgetID + "' could not be found by ID in getWidgetByID:"
                        +" Widgets in list by ID: "+m_widgets.toString() +" SESSIONID: "+getSession().getSessionID()
                        +" page name: "+getName();
                System.out.println(msg);
                return getSession().hookWidgetError(this, widgetID, msg, null);
            }
            String res = outputWidget(w.getID(), request); //use the getID, not the widgetID, since you can legally change the id property.
            return res;
        } catch (Exception e){
            String msg = sprintf("ERROR: [106] [getWidgetDiv] widgetID: %s  request:  %s error: %s",
                            new Object[]{widgetID, request, e});
            return getSession().hookWidgetError(this, widgetID, msg, e);
        }
    }

    public String getPagePropertyEval(String propertyName, String propertyValue, HttpServletRequest request)
    throws DynamideUncaughtException {
        Property property = (Property)getPropertiesTable().get(propertyName);
        if ( property == null ) {
            return "";
        }
        String sStyle = property.get("eval").toString();
        property.setValue(propertyValue);  // %% Note: String value only.
        setProperty(property);
        sStyle = expandTemplate(sStyle);
        return sStyle;
    }

    public String link(String moreParams, String linkText){
        return getSession().link(moreParams+"&page="+getID(), linkText);
    }



    //========== IPageChangeListener broadcaster ================
    private ArrayList m_pageChangeListeners = new ArrayList();

    public void addPageChangeListener(IPageChangeListener new_listener){
        m_pageChangeListeners.add(new_listener);
    }
    public void removePageChangeListener(IPageChangeListener listener){
        m_pageChangeListeners.remove(listener);
    }
    protected void notifyPageChangeListeners(String fieldName, Object oldValue, Object newValue){
        ChangeEvent event = new ChangeEvent();
        event.oldValue = oldValue;
        event.newValue = newValue;
        event.fieldName = fieldName;
        event.sender = this;

        int iSize = m_pageChangeListeners.size();
        for (int i = 0; i < iSize; i++){
            ((IPageChangeListener)m_pageChangeListeners.get(i)).pageChanged(event);
        }
    }
    protected void notifyPageNameChangeListeners(Object oldValue, Object newValue){
        ChangeEvent event = new ChangeEvent();
        event.oldValue = oldValue;
        event.newValue = newValue;
        event.fieldName = "name";
        event.sender = this;

        int iSize = m_pageChangeListeners.size();
        for (int i = 0; i < iSize; i++){
            ((IPageChangeListener)m_pageChangeListeners.get(i)).pageNameChanged(event);
        }
    }

    //===============================================================

    private RegisteredActions m_registeredActions = null;
    private void checkRegisteredActionsRef(){
        if ( m_registeredActions == null ) {
            m_registeredActions = new RegisteredActions(getSession());
        }
    }
    public RegisteredActions getRegisteredActions(){
        checkRegisteredActionsRef();
        return m_registeredActions;
    }
    public ScriptEvent fireRegisteredAction(String action){
        checkRegisteredActionsRef();
        return m_registeredActions.fireRegisteredAction(action);
    }
    public void registerAction(String action,
                               DynamideObject sender,
                               String eventName,
                               Object inputObject){
        checkRegisteredActionsRef();
        m_registeredActions.registerAction(action, sender, eventName, inputObject);
    }

    public void unregisterAction(String action){
        checkRegisteredActionsRef();
        m_registeredActions.unregisterAction(action);
    }

    //Changed this to have the Handler request object in the signature: it didn't use it.  7/25/2001
    public ScriptEvent handleAction(String action){
        ScriptEvent event = fireRegisteredAction(action);
        if (event != null && event.resultCode != ScriptEvent.RC_NO_EVENT_SOURCE){
            return event;
        }
        event = fire_onAction(action);
        return event;
    }

    public String getEventPrefixName(){
        String id = getName();
        int slash = id.lastIndexOf("/");
        if ( slash > -1 ) {
            return id.substring(slash+1);
        }
        return id;
    }

    public String eventShortNameToFullName(String eventShortName){
        return getEventPrefixName()+'_' + eventShortName;
    }

    public Object call(String eventShortName) {
        return call(eventShortName, new Object());
    }

    public Object call(String eventShortName, Object inputObject) {
        String eventName = eventShortNameToFullName(eventShortName);
        ScriptEventSource eventSource = super.getEventSource(eventName);
        if ( eventSource.source == null || eventSource.source.length() == 0) {
            eventSource = super.getEventSource(eventShortName);
            eventName = eventShortName;
        }
        return getSession().fireEvent(this, inputObject, eventName, ""/*pageID*/, ""/*nextPageID*/, ""/*action*/, eventSource, getFilename(), false, null, "").getOutputObject();
    }

    public ScriptEventSource getEventSource(String eventName, boolean addSignature){
        ScriptEventSource eventSource = super.getEventSource(eventName, addSignature);
        if ( eventSource.source.length()==0 ) {
            eventName = getEventPrefixName()+'_'+eventName;
            eventSource = super.getEventSource(eventName, addSignature);
        }
        return eventSource;
    }

    /** this version is so that widget authors and bean authors can fire their own custom events.
     *  %% TODO: how to let them populate the Event object...
     */
    public ScriptEvent fireEvent(String eventShortName){
        return fireEvent(null, eventShortName);
    }

    public ScriptEvent fireEvent(Object inputObject, String eventShortName) {
        ScriptEventSource eventSource = this.getEventSource(eventShortName);
        String action = "FIRED_FROM_WIDGET"; //todo: replace this and let widget author set it. %%
        ScriptEvent event = getSession().fireEvent(this, inputObject, eventSource.name, "", "", action, eventSource, getFilename(), false, null, "");
        return event;
    }

    public void fire_onImport(){
        ScriptEventSource eventSource = this.getEventSource("onImport", false);
        //logDebug("Page["+getFilename()+"].fire_onImport: "+eventSource);
        String eventName = eventSource.name;
        if ( eventSource.source.length()>0 ) {
            ScriptEvent event = getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), true);
        }
    }

    public ScriptEvent fire_onAction(String action){
        if ( action == null ) {
            action = "";
        }
        ScriptEventSource eventSource = this.getEventSource("onAction");
        ScriptEvent event = getSession().fireEvent(this, eventSource.name, "", "", action, eventSource, getFilename(), false);
        return event;
    }

    public ScriptEvent fire_onLoad(){
        ScriptEventSource eventSource = this.getEventSource("onLoad");
        return getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), false);
    }

    /** This event is fired just before expanding
     *  page and widget properties and returning the page to the browser.
     *  Nothing is done with the return values from the event:  it is strictly informational.
     *  However, you can set any errors, field values, etc.
     */
    public ScriptEvent fire_beforeOutput(){
        ScriptEventSource eventSource = this.getEventSource("beforeOutput");
        return getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), false);
    }

     public ScriptEvent fire_onOutput(){
        ScriptEventSource eventSource = this.getEventSource("onOutput");
        return getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), false);
    }


    public ScriptEvent fire_afterOutput(){
        ScriptEventSource eventSource = this.getEventSource("afterOutput");
        return getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), false);
    }

    /** This event is fired when a new page request has come in, and there was no action for this page,
     *  which can happen if you have a form with an action that also has hyperlinks or the user uses the 'back'
     *  button, skipping your form action.  If this page is a dialog and must set some result
     *  when the user is done seeing the dialog, this is the place to trap that event if they did
     *  not click on the button that fires the expected action.
     *  Nothing is done with the return values from the event:  it is strictly informational.
     *  However, you can set any errors, field values, etc. And you can perform necessary cleanup
     *  that would have been performed in onAction.
     */
    public ScriptEvent fire_onLeave(){
        ScriptEventSource eventSource = this.getEventSource("onLeave");
        return getSession().fireEvent(this, eventSource.name, "", "", "", eventSource, getFilename(), false);
    }

    /** @return true if any errors.
     */
    public boolean hasFieldErrors(){
        Enumeration en = m_widgets.objects();
        while (en.hasMoreElements()){
            Widget widget = (Widget)en.nextElement();
            Field aField = widget.getField();
            if ( aField != null ){
                //System.out.println("Widget: "+getName()+" Field: "+aField.dump());
                if (aField.getError() ) {
                    return true;
                }
            }
        }
        return false;
    }

    /** calls Page.clearError() to clear the page error, and calls Field.clearError for all Widgets
     * on this page.
     */
    public void clearErrors(){
        clearError();
        clearFieldErrors();
    }

    public void clearFieldErrors(){
        Enumeration en = m_widgets.objects();
        while (en.hasMoreElements()){
            Widget widget = (Widget)en.nextElement();
            Field aField = widget.getField();
            if ( aField != null ){
                aField.clearError();
            }
        }
    }

}
