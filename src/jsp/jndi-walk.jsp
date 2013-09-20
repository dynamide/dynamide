<%@  page language="java" contentType="text/html"
    import='java.util.*'
    import='javax.servlet.*'
    import='javax.servlet.http.*'
    import='javax.naming.*'
    import='java.io.PrintWriter'
%><html>
<head>
<style>
  h2 {font-family: "Arial,Helvetica,sans-serif";}
  pre {font-size: 8pt;}
  td {font-size: 10pt;}
</style>
</head>
<body bgcolor="white">
<jsp:directive.page velocity='true'/>
<%-- uncomment for params: <%@include file="showRequest.jsp"%> --%>

<%!

     //See also: com.dynamide.util.JNDIWalk, which is more recent than this jsp.

     public void walk(Object c, String indent, JspWriter out){
        try {
            if (c instanceof Context){
                Enumeration enum = ((Context)c).listBindings("");
                while (enum.hasMoreElements()){
                    Binding b = (Binding)enum.nextElement();
                    if (b!=null) {
                        out.println();
                        out.println(indent+"name: "+b.getName());
                        out.println(indent+"class: "+b.getClassName());
                        Object o = b.getObject();
                        if (o!=null){
                            out.println(indent+"object: "+o.toString());
                            walk(o, indent+"   ", out);
                        }
                    }
                }
            }
        } catch (Exception e){
            try {out.println("ERROR: [walk] "+e);} catch (Exception e2){System.out.println("ERROR: no jsp.out");}
        }
     }


%>


#{
  /**
   * Cached reference to the CourseHome interface.  Because this object
   * never changes, the client can look it up once in the <code>init()</code>
   * method and avoid JNDI calls for each request.
   */
    try {

      out.println("<h2>Servlet Context</h2>");
        ServletContext context = getServletContext();
        out.println("<table border='1' width='500'>");
        Enumeration atts = context.getAttributeNames();
        while (atts.hasMoreElements()){
            String s = (String)atts.nextElement();
            Object o = context.getAttribute(s);
            if (o!=null) {
                out.println("<tr><td>"+s + "</td><td>" + o.getClass().getName()+"</td><td>"+o.toString()+"</td></tr>");
            }
        }
        out.println("</table>");

        out.println("<h2>caucho.class.path</h2>");
      out.println("<pre>");
        StringTokenizer st = new StringTokenizer((String)context.getAttribute("caucho.class.path"), ";");
        while (st.hasMoreTokens()) {
            out.println("<br />"+st.nextToken());
        }
      out.println("</pre>");



      InitialContext ictx = new InitialContext();
      //Enumeration enum = ictx.list("java:comp");
      out.println("<h2>JNDI Context</h2>");
      out.println("<pre>");
      walk(ictx, "   ", out);
      out.println("</pre>");

      out.println("<hr />");

      //Context ctx = (Context)ictx.lookup("java:comp/env/scheduler");
      //if (ctx!=null){
      //  home = (RuleDefLocalHome) ctx.lookup("RuleDefLocalHome");
      ///  ruledef = (RuleDef)home.findByPrimaryKey(1);
      //}
    } catch (Exception e) {
      out.print("ERROR: "+e);
    }
}#

</body>
</html>