package com.dynamide.apps.ide;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.dynamide.DynamideObject;
import com.dynamide.JDOMFile;
import com.dynamide.Page;
import com.dynamide.Session;
import com.dynamide.Widget;
import com.dynamide.event.ScriptEvent;

import com.dynamide.resource.Assembly;
import com.dynamide.resource.ResourceManager;

import com.dynamide.util.StringList;
import com.dynamide.util.StringTools;
import com.dynamide.util.Tools;

public class IDEApp extends DynamideObject {

    public IDEApp(DynamideObject owner){
        super(owner);
    }

    public ScriptEvent application_onAction(ScriptEvent event, Session session)
    throws Exception {
        String action = event.action;
        String subsessionID = event.getQueryParam("SUBSESSIONID");
        if (subsessionID == null || subsessionID.length()==0){
            subsessionID = event.session.getFieldStringValue("SUBSESSIONID");
        }
        Session subsession = event.session.findSession(subsessionID);
        System.out.println("\r\n\r\n\r\n In IDEApp.application_onAction: "+action+" sessionid: "+session.getSessionID()+" subsessionID: "+subsessionID);
        String body = "";
        boolean fullbody = false; //set to false means wrap result in html tag.
        if (action.equals("getPageList")){
            Widget wd = new Widget(subsession, subsession);
            //wd.setSession(subsession);
            wd.setSession(event.session);
            try {
                wd.setType("com.dynamide.ide.pagelist");
                wd.setProperty("dropdown", "true");
                wd.setProperty("name", "pagelist1");
                String selectName = event.getQueryParam("selectName");
                wd.setProperty("selectName", selectName);
                StringList pageList = subsession.getPageList();
                wd.setNamedObject("pagelist", pageList);
                body = wd.render();
            } catch (Exception e) {
                body = "ERROR: "+Tools.errorToString(e, true);
                event.println(body);
            }
        } else if (action.equals("getPaletteList")){
            if (subsession == null) {
                body = "ERROR: subsession is null: "+subsessionID;
                event.println(body);
            } else {
                Map map = subsession.getAssembly().listTemplates("resources/widgets");
                //System.out.println("[[[[[[[[[[[[[[[[[[ map: "+map);
                Set keys = map.keySet();
                StringList paletteList = new StringList(keys);
                List importsList = subsession.getAssembly().getImports();
                Object o;
                Assembly assm;
                for ( Iterator it = importsList.iterator(); it.hasNext(); ) {
                    assm = (Assembly)it.next();
                    paletteList.add("--- "+assm.toString()+" ---");
                    Map importMap = assm.listTemplates("resources/widgets");
                    Set importKeys = importMap.keySet();
                    paletteList.addAll(importKeys);
                    //System.out.println("[[[[[[[[[[[[[[[[[[ keys: "+importKeys);
                }



                Widget wd = new Widget(subsession, subsession);
                //wd.setSession(subsession);
                wd.setSession(event.session);
                wd.setType("com.dynamide.ide.palettelist");
                wd.setID("paletteList1"); //wd.setProperty("name", "paletteList1");
                String selectName = event.getQueryParam("selectName");
                wd.setProperty("selectName", selectName);
                wd.setNamedObject("list", paletteList);
                body = wd.render();
                //logDebug("paletteList: "+body);
            }
        } else if (action.equals("getPagetypeList")){
            if (subsession == null) {
                body = "ERROR: subsession is null: "+subsessionID;
                event.println(body);
            } else {
                Map map = subsession.getAssembly().listTemplates("resources/widgets/pagetypes");
                event.println("map: "+map);
                Set keys = map.keySet();
                StringList pagetypeList = new StringList(keys);

                List importsList = subsession.getAssembly().getImports();
                Object o;
                Assembly assm;
                for ( Iterator it = importsList.iterator(); it.hasNext(); ) {
                    assm = (Assembly)it.next();
                    Map importMap = assm.listTemplates("resources/widgets/pagetypes");
                    Set importKeys = importMap.keySet();
                    pagetypeList.addAll(importKeys);
                    pagetypeList.add("---------------");
                }
                Widget wd = new Widget(subsession, subsession);
                //wd.setSession(subsession);
                wd.setSession(event.session);
                wd.setType("com.dynamide.ide.pagetypelist");
                wd.setProperty("name", "pagetypeList1");
                String selectName = event.getQueryParam("selectName");
                wd.setProperty("selectName", selectName);
                wd.setNamedObject("list", pagetypeList);
                body = wd.render();
            }
        } else if (action.equals("getWidgetPrototype")){
            try {
                //event.println("\r\nIN getWidgetPrototype");
                String widType = event.getQueryParam("widgetType");
                String targetPageID = event.getQueryParam("targetPageID");
                String widgetName = subsession.createWidget(widType, targetPageID);
                body = subsession.renderWidget(widgetName, targetPageID, event.request);
                subsession.setFieldValue("LAST_WIDGET", widgetName);
                //the above line enables a hack to show the Inspector just after a widget is dropped.
                //See also: idetop.xml::showInspector() and idetop.xml::designModeClick, or grep for "LAST_WIDGET"
                String format = event.getQueryParam("format");
                if (format.equals("debug")){
                    String debugHeader =
                                        "Widget Type: "+widType
                                        +"<br/>SESSIONID: "+subsession.getSessionID()
                                        +"<br/>Widget Name: "+widgetName
                                        +"<br/>Target Page: "+targetPageID;
                    try {
                        body = JDOMFile.prettyPrintHTML(body);
                        // %% Uggg. these &apos; seem to be introduced by prettyPrintHTML, although i put them in in other places...
                        body = StringTools.searchAndReplaceAll(body, "&apos;", "'"); // %% Hack until JDom beta 7, where they fix this.
                        String escapedBody = com.dynamide.util.StringTools.searchAndReplaceAll(body, "<", "&lt;");
                        escapedBody = com.dynamide.util.StringTools.searchAndReplaceAll(escapedBody, ">", "&gt;");
                        String previewBody = com.dynamide.util.StringTools.searchAndReplaceAll(body, "&amp;", "&");
                        body = "<head><title>Widget Prototype ["+widgetName+"]</title>"
                              +subsession.getInclude("resources/js/page.js")
                            +"</head><body bgcolor='#A4BFDD'>"
                            +debugHeader
                            +"<hr/><center><font size='+1'>Source:</font></center><hr/><br/>"
                            +"<pre>"+escapedBody+"</pre><hr/><center><font size='+1'>Preview:</font></center><hr/><br/>"
                            +previewBody
                            +"</body>";
                    } catch (com.dynamide.XMLFormatException xe){
                        body = "<head><title>Widget Prototype ["+widgetName+"] (with error)</title></head><body bgcolor='#A4BFDD'>"
                            +debugHeader
                            +"<br/><br/>"
                            +xe.getMessage()
                            +"</center><hr/><br/>"
                            +"</body>";
                    }
                } else {
                    event.println("getWidgetPrototype: \r\n"+body); //log the error
                }
            } catch (Exception e2) {
                body = "ERROR: "+Tools.errorToString(e2, true);
                event.println("\r\n\r\n===========\r\n"+body);
            }
        } else if (action.equals("startSubsession")){
            String path = event.getQueryParam("projectPath"); //a uri, actually.
            body = handleStartSubsession(event, path);
            fullbody = true;
        } else if (action.equals("clearWidgetTypePool")){
            //So the IDE property editor can pick up changes in the property list also:
            event.session.clearWidgetTypePool();
            // So the generated prototypes can pick up the changes in the design session:
            subsession.clearWidgetTypePool();
            body = "Widget Type Pool cleared";
        } else if (action.equals("showInspector")){
            event.resultAction = ScriptEvent.RA_JUMP_TO_PAGE;
            event.nextPageID = "inspector";
        } else if (action.equals("showEval")) {
            Page newPage = event.session.getPageByID("evaltop");
            StringList variables = new StringList();
            variables.addObject("evalResult", "");
            variables.addObject("source", "");
            body = newPage.outputPage(variables);
            fullbody = true;
        } else if (action.equals("eval")) {
            Page newPage = event.session.getPageByID("evaltop");
            String USER = event.getQueryParam("USER");
            String source = event.getQueryParam("source");
            String evalResult;
            if (source.length()==0){
                evalResult = "ERROR: 'source' parameter was empty.";
                event.println(evalResult);
            } else {
                evalResult = Session.handleEval(subsessionID, source, USER, event.request);
            }
            event.println("evalResult: \r\n"+evalResult);
            if (evalResult.startsWith("ERROR")){
                evalResult = StringTools.escape(evalResult);
                event.println(evalResult);
                body = "<html><body>"+evalResult+"</body></html>";
            } else {
                StringList variables = new StringList();
                variables.addObject("evalResult", evalResult);
                variables.addObject("source", source);
                body = newPage.outputPage(variables);
            }
            fullbody = true;
        } else if (action.equals("newPage")) {
            String newPageID = event.getQueryParam("newPageID");
            String newPagetype = event.getQueryParam("newPagetype");
            logDebug("================== newPage: newPageID is "+newPageID);
            try {
                Page newPage = subsession.newPage(newPageID, newPagetype);
                body = newPage.outputPage().getResult();
                fullbody = true;
            } catch (Exception e){
                body = "<html><body>ERROR: "+e+"</body></html>";
                event.println("ERROR: "+e);
                fullbody = true;
            }
        } else if (action.equals("newProject")) {
            //we come in here when newProjectDlg.xml POSTs
            ResourceManager rm = event.session.getResourceManager();
            String urlPath = event.getQueryParam("urlPath");
            String appName = event.getQueryParam("appName");
            String account = event.getQueryParam("account"); //can be the base site, or the user's site
            String build = event.getQueryParam("build");
            boolean loadOnStartup = Tools.isTrue(event.getQueryParam("loadOnStartup"));
            String assemblyInterface = event.getQueryParam("assemblyInterface");
            String assemblyBasename = event.getQueryParam("assemblyBasename");
            try {
                Assembly assembly = rm.findAssemblyInAccount(account, assemblyBasename, assemblyInterface, build);
                if ( assembly == null ) {
                    body = "ERROR: assembly not found: "+account+", "+assemblyBasename+", "+assemblyInterface+", "+build;
                } else {
                    rm.createApplication(assembly, appName);
                    rm.addWebApp( assembly,
                              urlPath,
                              appName,
                              loadOnStartup
                              );
                    body = "Created new application: "+urlPath;
                }
            } catch (Exception e){
                body = Tools.errorToString(e, true);
                event.resultSrc = "<html><body>"+body+"</body></html>";
                event.resultAction = ScriptEvent.RA_RETURN_SOURCE;
                event.println(body);
            }
        } else if (action.equals("saveProject")) {
            boolean result = subsession.saveToFile();
            if (result){
                body = "Project Saved";
            } else {
                body = "ERROR: couldn't save project";
                event.println(body);
            }
        } else if (action.equals("renamePage")) {
            String targetPageID = event.getQueryParam("targetPageID");
            if (subsession == null){
                body = "ERROR: subsession is null";
            } else {
                Page targetPage = subsession.findPageByID(targetPageID);
                if (targetPage == null){
                    body = "ERROR: targetPageID not found in project: "+targetPageID;
                } else {
                    targetPage.saveToFile(); //does not call removeUnusedWidgets anymore.
                    // %% replace this with a call the existing overload saveToFile(name...)
                    body = "OK";
                }
            }

        }
        if (body.length() >0){
            if (!fullbody) {
                event.resultSrc = "<html>"+body+"</html>";  //you always need the html tag, the body doesn't have to have a body tag, though.
            } else {
                event.resultSrc = body;
            }
            event.resultAction = ScriptEvent.RA_RETURN_SOURCE;
        }
        return event;
    }

    public String handleStartSubsession(com.dynamide.event.ScriptEvent event, String path){
        String body;
        try {
            event.println("application.xml startSubsession. projectPath: "+path);
            String debugQueryString = "";
            String ssid = event.getQueryParam("SUBSESSIONID");
            if (!StringTools.isEmpty(ssid)){
            	/*int hasQuestionMark = path.indexOf("?");
            	if (hasQuestionMark==-1)
            		debugQueryString = "?SESSIONID="+ssid;
        		else 
        			debugQueryString = "&SESSIONID="+ssid;
            	path = path + param;
            	*/
            	debugQueryString = "?SESSIONID="+ssid;
            }
            body = event.session.createNewSession(path, true, debugQueryString); //you need the request for things like wwwroot.
            event.session.setFieldValue("SUBSESSIONID", body); //body is just the sessionID.
            event.println("application.xml newSession: "+body);
            event.println("application.xml ide sessionid: "+event.session.getSessionID());
        } catch (Exception e3){
            body = "ERROR: "+Tools.errorToString(e3, true);
            event.println(body);
        }
        return body;
    }

    private TreeMap m_designers = new TreeMap();

    public Designer findOrCreateDesigner(String id){
        Designer result = (Designer)m_designers.get(id);
        if ( result == null ) {
            result = new Designer();
            result.setID(id);
            m_designers.put(id, result);
        }
        return result;
    }
}
