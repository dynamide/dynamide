package com.dynamide.resource;

import com.dynamide.util.FileTools;
import com.dynamide.util.Tools;

import org.apache.log4j.Priority;

import java.util.Vector;
import java.util.Iterator;

public class HtmlLogger implements ILogger {
	public String toString(){
		return "HtmlLogger:"+m_url+';'+m_realDirectory+';'+m_errorIndex;
	}
	private String m_url = "";
	private String m_realDirectory = "";
	private Vector m_errorIndex = new Vector();
	
	private class ErrorRow {
		String timestamp = Tools.nowLocale();
        String sessionid;
        String applicationid;
        String threadid;
        String account;
        String login;
        String resourcename;
        String stacktrace;
        String message;
        String exceptionName;
        String shortMessage;
        String errorURI;
        public String toString(){
        	return "ErrorRow:"+timestamp+';'+sessionid+';'+applicationid+';'+threadid+';'+exceptionName+';'+shortMessage+';'+errorURI;
        }
	}

	public void connect(ResourceManager rm, String configString) throws Exception {
		m_url = rm.getErrorLogsURI();
		m_realDirectory = rm.getErrorLogsDir();
	}

	public void log(org.apache.log4j.Priority priority,
            String sessionid,
            String threadid,
            String stacktrace,
            String message,
            String errorURI)
	{
		ErrorRow r = new ErrorRow();
		r.sessionid = sessionid;
        r.applicationid = "";
        r.threadid = threadid;
        r.account = "";
        r.login = "";
        r.resourcename = "";
        r.stacktrace = stacktrace;
        r.message = message;
        r.exceptionName = "";
        r.shortMessage = message.substring(0, 20)+"...";
        r.errorURI = errorURI;
        m_errorIndex.add(r);		
	}

	public void log(org.apache.log4j.Priority priority,
            String sessionid,
            String applicationid,
            String threadid,
            String account,
            String login,
            String resourcename,
            String stacktrace,
            String message,
            String exceptionName,
            String shortMessage,
            String errorURI)
	{
		ErrorRow r = new ErrorRow();
		r.sessionid = sessionid;
        r.applicationid = applicationid;
        r.threadid = threadid;
        r.account = account;
        r.login = login;
        r.resourcename = resourcename;
        r.stacktrace = stacktrace;
        r.message = message;
        r.exceptionName = exceptionName;
        r.shortMessage = shortMessage;
        r.errorURI = errorURI;
        m_errorIndex.add(r);	
        //System.out.println("HtmlLogger adding: "+r);
	}
	
	public synchronized void flush(){
		String content = generateIndex();
		java.io.File f = FileTools.saveFile(m_realDirectory, "index.html", content);	
	}
	
	/**
	 * @return full path of the newly rolled file, or "" if not rolled.
	 */
	public synchronized String rollLog() {
		flush();
		m_errorIndex.clear();
		//TODO: make the index file roll, and put in a link in the new index to the old index
		//String newName = FileTools.generateNextFilename(m_realDirectory, "index.html", "index", "html");
		//public static File saveToTempFile(String directory, String content){
		try {
			if (FileTools.fileExists(FileTools.join(m_realDirectory, "index.html"))){
				return FileTools.copyToTempFile(m_realDirectory, "index", "html");
			}
		} catch (Exception e){
			System.err.println("Couldn't make error index in HtmlLogger.generateIndex"+com.dynamide.util.Tools.errorToString(e, true, true));
		}
		return "";
		//TODO: make sure there is a new index file to point to the old ones.
		//e.g. generateIndex() or flush() again.
	}
	
	private void wr(StringBuffer r, String cell){
		r.append("<td>"+cell+"</td>");
	}
	private String generateIndex(){
		StringBuffer r = new StringBuffer("<html><body>");
		r.append("<table border='1' cellpadding='0' cellspacing='0'>");
		Iterator it = m_errorIndex.iterator();
		while (it.hasNext()) {
			ErrorRow row = (ErrorRow)it.next();
			r.append("<tr>");
			wr(r,row.timestamp);
			wr(r,row.sessionid);
			wr(r,row.applicationid);
			wr(r,row.threadid);
			wr(r,row.account);
			wr(r,row.login);
			wr(r,row.resourcename);
			//wr(r,row.stacktrace);
			wr(r,row.exceptionName);
			wr(r,row.shortMessage);
			wr(r,"<a href='"+row.errorURI+"'>error</a>");
			r.append("</tr>");
		}
		r.append("</table>");
		//r.append("<br />See <a href='"+newName+"'>previous log</a><br />");
		r.append("</body></html>");
		return r.toString();
	}

}
