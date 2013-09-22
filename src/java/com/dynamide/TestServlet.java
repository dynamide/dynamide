package com.dynamide;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TestServlet() {
    }

    public void init()
    throws ServletException {
        try {
            System.out.println("\r\n***** TestServlet init() *****\r\n");
            ServletContext context = getServletContext();
            String DYNAMIDE_HOME =  getInitParameter(Constants.DYNAMIDE_HOME);
            System.out.println("a @@@@@@@@@@@@@@@@@@@@@@@@ init param: DYNAMIDE_HOME: "+DYNAMIDE_HOME);
        } catch (Throwable t){
                System.err.println("ERROR: "+t);
        }
    }

    public void doGet(HttpServletRequest  req, HttpServletResponse response) throws ServletException, IOException{
        doGetPost(req, response);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        doGetPost(req, response);
    }

    private static final boolean PARANOID_GC = false;  //do not turn this on in jdk 1.4 esp. on Solaris, it makes GC be concurrent!

    public void doGetPost(HttpServletRequest  request, HttpServletResponse response) throws ServletException, IOException{
        String servletAction = request.getParameter("servletAction");
        printResult(response, "text/html", "OK: action: "+servletAction+"\r\n<br />");
    }

    private void printResult(HttpServletResponse response, String contentType, String result)
    throws IOException {
        PrintWriter out = response.getWriter();
        try {
            response.setContentType("text/html");
            dumpInitParameters(out);
            out.print(result);
        } finally {
            out.close();
        }
    }

    private void dumpInitParameters(PrintWriter pw){
        Enumeration e = getInitParameterNames();

        if (! e.hasMoreElements()) {
            return;
        }

        pw.println("<h1>Servlet Init Parameters:</h1>");
        pw.println("<table border='1' cellpadding='0' cellspacing='0'>");

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            pw.print("<tr><td>");
            pw.print(name);
            pw.print("</td><td>");
            pw.println(getInitParameter(name));
            pw.print("</td></tr>");
        }

        pw.println("</table>");
    }



}
