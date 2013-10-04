package com.dynamide.apps.ide;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Page;
import com.dynamide.Property;
import com.dynamide.Session;
import com.dynamide.Widget;
import com.dynamide.XMLFormatException;
import com.dynamide.datatypes.Datatype;
import com.dynamide.datatypes.DatatypeException;
import com.dynamide.event.ScriptEvent;

import com.dynamide.resource.Assembly;
import com.dynamide.resource.ResourceManager;

import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class IDEApp extends DynamideObject {

    public IDEApp(DynamideObject owner) {
        super(owner);
    }

    private TreeMap m_designers = new TreeMap();

    public Designer findOrCreateDesigner(String id) {
        Designer result = (Designer) m_designers.get(id);
        if (result == null) {
            result = new Designer();
            result.setID(id);
            m_designers.put(id, result);
        }
        return result;
    }

    private class ActionInParams {
        String action;
        String subsessionID;
        Session subsession;
        ScriptEvent event;
        public ActionInParams(String action, String subsessionID, Session subsession, ScriptEvent event) {
            this.action = action;
            this.subsessionID = subsessionID;
            this.subsession = subsession;
            this.event = event;
        }
    }

    private class ActionOutParams {
        String body = "";
        boolean fullbody = false;    //set to false means wrap result in html tag.
    }

    public ScriptEvent application_onAction(ScriptEvent event, Session session) throws Exception {
        String action = event.action;
        String subsessionID = event.getQueryParam("SUBSESSIONID");
        if (subsessionID == null || subsessionID.length() == 0) {
            subsessionID = event.session.getFieldStringValue("SUBSESSIONID");
        }
        Session subsession = event.session.findSession(subsessionID);
        System.out.println("\r\n\r\n\r\n In IDEApp.application_onAction: " + action + " sessionid: " + session.getSessionID() + " subsessionID: " + subsessionID);

        ActionInParams in = new ActionInParams(action, subsessionID, subsession, event);
        ActionOutParams out = new ActionOutParams();

        if (action.equals("getPageList")) {
            getPageList(in, out);
        } else if (action.equals("getPaletteList")) {
            getPaletteList(in, out);
        } else if (action.equals("getPagetypeList")) {
            getPagetypeList(in, out);
        } else if (action.equals("getWidgetPrototype")) {
            getWidgetPrototype(in, out);
        } else if (action.equals("startSubsession")) {
            startSubsession(in, out);
        } else if (action.equals("clearWidgetTypePool")) {
            clearWidgetTypePool(in, out);
        } else if (action.equals("showInspector")) {
            showInspector(in, out);
        } else if (action.equals("showEval")) {
            showEval(in, out);
        } else if (action.equals("eval")) {
            eval(in, out);
        } else if (action.equals("newPage")) {
            newPage(in, out);
        } else if (action.equals("newProject")) {
            newProject(in, out);
        } else if (action.equals("saveProject")) {
            saveProject(in, out);
        } else if (action.equals("renamePage")) {
            renamePage(in, out);
        } else if (action.equals("SaveLayoutView")) {
            SaveLayoutView(in, out);
        } else if (action.equals("setApplicationProperty")) {
            setApplicationProperty(in, out);
        }

        if (out.body.length() > 0) {
            if (!out.fullbody) {
                event.resultSrc = "<html>" + out.body + "</html>";  //you always need the html tag, the body doesn't have to have a body tag, though.
            } else {
                event.resultSrc = out.body;
            }
            event.resultAction = ScriptEvent.RA_RETURN_SOURCE;
        }
        return event;
    }

    //============== BEGIN Action handlers =======================================================

    private void getPageList(ActionInParams in, ActionOutParams out) {
        Widget wd = new Widget(in.subsession, in.subsession);
        //wd.setSession(in.subsession);
        wd.setSession(in.event.session);
        try {
            wd.setType("com.dynamide.ide.pagelist");
            wd.setProperty("dropdown", "true");
            wd.setProperty("name", "pagelist1");
            String selectName = in.event.getQueryParam("selectName");
            wd.setProperty("selectName", selectName);
            StringList pageList = in.subsession.getPageList();
            wd.setNamedObject("pagelist", pageList);
            out.body = wd.render();
        } catch (Exception e) {
            out.body = "ERROR: " + Tools.errorToString(e, true);
            in.event.println(out.body);
        }
    }

    private void getPaletteList(ActionInParams in, ActionOutParams out) throws Exception {
        if (in.subsession == null) {
            out.body = "ERROR: in.subsession is null: " + in.subsessionID;
            in.event.println(out.body);
        } else {
            Map map = in.subsession.getAssembly().listTemplates("resources/widgets");
            //System.out.println("[[[[[[[[[[[[[[[[[[ map: "+map);
            Set keys = map.keySet();
            StringList paletteList = new StringList(keys);
            List importsList = in.subsession.getAssembly().getImports();
            Object o;
            Assembly assm;
            for (Iterator it = importsList.iterator(); it.hasNext(); ) {
                assm = (Assembly) it.next();
                paletteList.add("--- " + assm.toString() + " ---");
                Map importMap = assm.listTemplates("resources/widgets");
                Set importKeys = importMap.keySet();
                paletteList.addAll(importKeys);
                //System.out.println("[[[[[[[[[[[[[[[[[[ keys: "+importKeys);
            }

            Widget wd = new Widget(in.subsession, in.subsession);
            //wd.setSession(in.subsession);
            wd.setSession(in.event.session);
            wd.setType("com.dynamide.ide.palettelist");
            wd.setID("paletteList1"); //wd.setProperty("name", "paletteList1");
            String selectName = in.event.getQueryParam("selectName");
            wd.setProperty("selectName", selectName);
            wd.setNamedObject("list", paletteList);
            out.body = wd.render();
            //logDebug("paletteList: "+out.body);
        }
    }

    private void getPagetypeList(ActionInParams in, ActionOutParams out) throws Exception {
        if (in.subsession == null) {
            out.body = "ERROR: in.subsession is null: " + in.subsessionID;
            in.event.println(out.body);
        } else {
            Map map = in.subsession.getAssembly().listTemplates("resources/widgets/pagetypes");
            in.event.println("map: " + map);
            Set keys = map.keySet();
            StringList pagetypeList = new StringList(keys);

            List importsList = in.subsession.getAssembly().getImports();
            Object o;
            Assembly assm;
            for (Iterator it = importsList.iterator(); it.hasNext(); ) {
                assm = (Assembly) it.next();
                Map importMap = assm.listTemplates("resources/widgets/pagetypes");
                Set importKeys = importMap.keySet();
                pagetypeList.addAll(importKeys);
                pagetypeList.add("---------------");
            }
            Widget wd = new Widget(in.subsession, in.subsession);
            //wd.setSession(in.subsession);
            wd.setSession(in.event.session);
            wd.setType("com.dynamide.ide.pagetypelist");
            wd.setProperty("name", "pagetypeList1");
            String selectName = in.event.getQueryParam("selectName");
            wd.setProperty("selectName", selectName);
            wd.setNamedObject("list", pagetypeList);
            out.body = wd.render();
        }
    }

    private void getWidgetPrototype(ActionInParams in, ActionOutParams out) {
        try {
            //in.event.println("\r\nIN getWidgetPrototype");
            String widType = in.event.getQueryParam("widgetType");
            String targetPageID = in.event.getQueryParam("targetPageID");
            String widgetName = in.subsession.createWidget(widType, targetPageID);
            out.body = in.subsession.renderWidget(widgetName, targetPageID, in.event.request);
            in.subsession.setFieldValue("LAST_WIDGET", widgetName);
            //the above line enables a hack to show the Inspector just after a widget is dropped.
            //See also: idetop.xml::showInspector() and idetop.xml::designModeClick, or grep for "LAST_WIDGET"
            String format = in.event.getQueryParam("format");
            if (format.equals("debug")) {
                String debugHeader = "Widget Type: " + widType + "<br/>SESSIONID: " + in.subsession.getSessionID() + "<br/>Widget Name: " + widgetName + "<br/>Target Page: " + targetPageID;
                try {
                    out.body = JDOMFile.prettyPrintHTML(out.body);
                    // %% Uggg. these &apos; seem to be introduced by prettyPrintHTML, although i put them in in other places...
                    out.body = StringTools.searchAndReplaceAll(out.body, "&apos;", "'"); // %% Hack until JDom beta 7, where they fix this.
                    String escapedBody = com.dynamide.util.StringTools.searchAndReplaceAll(out.body, "<", "&lt;");
                    escapedBody = com.dynamide.util.StringTools.searchAndReplaceAll(escapedBody, ">", "&gt;");
                    String previewBody = com.dynamide.util.StringTools.searchAndReplaceAll(out.body, "&amp;", "&");
                    out.body = "<head><title>Widget Prototype [" + widgetName + "]</title>" + in.subsession.getInclude("resources/js/page.js") + "</head><out.body bgcolor='#A4BFDD'>" + debugHeader + "<hr/><center><font size='+1'>Source:</font></center><hr/><br/>" + "<pre>" + escapedBody + "</pre><hr/><center><font size='+1'>Preview:</font></center><hr/><br/>" + previewBody + "</out.body>";
                } catch (com.dynamide.XMLFormatException xe) {
                    out.body = "<head><title>Widget Prototype [" + widgetName + "] (with error)</title></head><out.body bgcolor='#A4BFDD'>" + debugHeader + "<br/><br/>" + xe.getMessage() + "</center><hr/><br/>" + "</out.body>";
                }
            } else {
                in.event.println("getWidgetPrototype: \r\n" + out.body); //log the error
            }
        } catch (Exception e2) {
            out.body = "ERROR: " + Tools.errorToString(e2, true);
            in.event.println("\r\n\r\n===========\r\n" + out.body);
        }
    }

    private void startSubsession(ActionInParams in, ActionOutParams out) {
        String path = in.event.getQueryParam("projectPath"); //a uri, actually.
        try {
            in.event.println("application.xml startSubsession. projectPath: " + path);
            String debugQueryString = "";
            String ssid = in.event.getQueryParam("SUBSESSIONID");
            if (!StringTools.isEmpty(ssid)) {
                /*int hasQuestionMark = path.indexOf("?");
                    if (hasQuestionMark==-1)
                        debugQueryString = "?SESSIONID="+ssid;
                    else
                        debugQueryString = "&SESSIONID="+ssid;
                    path = path + param;
                    */
                debugQueryString = "?SESSIONID=" + ssid;
            }
            out.body = in.event.session.createNewSession(path, true, debugQueryString); //you need the request for things like wwwroot.
            in.event.session.setFieldValue("SUBSESSIONID", out.body); //out.body is just the sessionID.
            in.event.println("application.xml newSession: " + out.body);
            in.event.println("application.xml ide sessionid: " + in.event.session.getSessionID());
        } catch (Exception e3) {
            out.body = "ERROR: " + Tools.errorToString(e3, true);
            in.event.println(out.body);
        }
        out.fullbody = true;
    }

    private void clearWidgetTypePool(ActionInParams in, ActionOutParams out) {
        //So the IDE property editor can pick up changes in the property list also:
        in.event.session.clearWidgetTypePool();
        // So the generated prototypes can pick up the changes in the design session:
        in.subsession.clearWidgetTypePool();
        out.body = "Widget Type Pool cleared";
    }

    private void showInspector(ActionInParams in, ActionOutParams out) {
        in.event.resultAction = ScriptEvent.RA_JUMP_TO_PAGE;
        in.event.nextPageID = "inspector";
    }

    private void showEval(ActionInParams in, ActionOutParams out) throws Exception {
        Page newPage = in.event.session.getPageByID("eval");
        StringList variables = new StringList();
        variables.addObject("evalResult", "");
        variables.addObject("source", "");
        out.body = newPage.outputPage(variables);
        out.fullbody = true;
    }

    private void eval(ActionInParams in, ActionOutParams out) throws Exception {
        Page newPage = in.event.session.getPageByID("eval");
        String USER = in.event.getQueryParam("USER");
        String source = in.event.getQueryParam("source");
        String evalResult;
        if (source.length() == 0) {
            evalResult = "ERROR: 'source' parameter was empty.";
            in.event.println(evalResult);
        } else {
            evalResult = Session.handleEval(in.subsessionID, source, USER, in.event.request);
        }
        in.event.println("evalResult: \r\n" + evalResult);
        if (evalResult.startsWith("ERROR")) {
            evalResult = StringTools.escape(evalResult);
            in.event.println(evalResult);
            out.body = "<html><out.body>" + evalResult + "</out.body></html>";
        } else {
            StringList variables = new StringList();
            variables.addObject("evalResult", evalResult);
            variables.addObject("source", source);
            out.body = newPage.outputPage(variables);
        }
        out.fullbody = true;
    }

    private void newPage(ActionInParams in, ActionOutParams out) {
        String newPageID = in.event.getQueryParam("newPageID");
        String newPagetype = in.event.getQueryParam("newPagetype");
        logDebug("================== newPage: newPageID is " + newPageID);
        try {
            Page newPage = in.subsession.newPage(newPageID, newPagetype);
            out.body = newPage.outputPage().getResult();
            out.fullbody = true;
        } catch (Exception e) {
            out.body = "<html><out.body>ERROR: " + e + "</out.body></html>";
            in.event.println("ERROR: " + e);
            out.fullbody = true;
        }
    }

    private void newProject(ActionInParams in, ActionOutParams out) {
        //we come in here when newProjectDlg.xml POSTs
        ResourceManager rm = in.event.session.getResourceManager();
        String urlPath = in.event.getQueryParam("urlPath");
        String appName = in.event.getQueryParam("appName");
        String account = in.event.getQueryParam("account"); //can be the base site, or the user's site
        String build = in.event.getQueryParam("build");
        boolean loadOnStartup = Tools.isTrue(in.event.getQueryParam("loadOnStartup"));
        String assemblyInterface = in.event.getQueryParam("assemblyInterface");
        String assemblyBasename = in.event.getQueryParam("assemblyBasename");
        try {
            Assembly assembly = rm.findAssemblyInAccount(account, assemblyBasename, assemblyInterface, build);
            if (assembly == null) {
                out.body = "ERROR: assembly not found: " + account + ", " + assemblyBasename + ", " + assemblyInterface + ", " + build;
            } else {
                rm.createApplication(assembly, appName);
                rm.addWebApp(assembly, urlPath, appName, loadOnStartup);
                out.body = "Created new application: " + urlPath;
            }
        } catch (Exception e) {
            out.body = Tools.errorToString(e, true);
            in.event.resultSrc = "<html><out.body>" + out.body + "</out.body></html>";
            in.event.resultAction = ScriptEvent.RA_RETURN_SOURCE;
            in.event.println(out.body);
        }
    }

    private void saveProject(ActionInParams in, ActionOutParams out) {
        boolean result = in.subsession.saveToFile();
        if (result) {
            out.body = "Project Saved";
        } else {
            out.body = "ERROR: couldn't save project";
            in.event.println(out.body);
        }
    }

    private void renamePage(ActionInParams in, ActionOutParams out) {
        String targetPageID = in.event.getQueryParam("targetPageID");
        if (in.subsession == null) {
            out.body = "ERROR: in.subsession is null";
        } else {
            Page targetPage = in.subsession.findPageByID(targetPageID);
            if (targetPage == null) {
                out.body = "ERROR: targetPageID not found in project: " + targetPageID;
            } else {
                targetPage.saveToFile(); //does not call removeUnusedWidgets anymore.
                // %% replace this with a call the existing overload saveToFile(name...)
                out.body = "OK";
            }
        }
    }

    private void SaveLayoutView(ActionInParams in, ActionOutParams out) throws Exception {
        //This is untested code.  Moved here 2013-10-03 from idehidden.xml, but there's no caller in the source tree.
        String pagepost = in.event.getQueryParam("htmlSource");
        String targetPageID = in.event.getQueryParam("targetPageID");

        if (in.subsession == null) {
            in.event.println("in.subsession was null");
        } else {
            Page targetPage = in.subsession.getPageByID(targetPageID);
            if (targetPage != null) {
                try {
                    targetPage.setHTMLSource(pagepost);                //throws XMLFormatException
                    if (targetPage.saveToFile()) {                      //This flushes the whole XML in-memory tree to disk.
                        in.event.println("Page " + targetPageID + " saved");
                    } else {
                        in.event.println("ERROR: couldn't save page " + targetPageID);
                    }
                } catch (XMLFormatException xmle) {
                    in.event.println(xmle.getMessage());
                    out.body = "ERROR: " + xmle.getMessage();
                }
            }
        }
    }

    private void setApplicationProperty(ActionInParams in, ActionOutParams out) throws Exception {
         String name = in.event.getQueryParam("propertyName");
         String value = in.event.getQueryParam("propertyValue");
         String propertyDataType = in.event.getQueryParam("propertyDataType");
        System.out.println("===IDEApp.setApplicationProperty==(questionable)===]]] "+propertyDataType);   // %%TODO: what about other types???
        Datatype d = (Datatype)Class.forName(propertyDataType).newInstance();
        d.set("value", value);
        Property p = new Property(in.event.session, "value", d);
        in.subsession.setProperty(name, p);
        out.body = in.subsession.getPropertyStringValue(name);
    }
    
    //============== END Action handlers =======================================================
}
