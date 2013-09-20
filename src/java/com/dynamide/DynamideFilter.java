package com.dynamide;

import java.io.IOException;

import javax.servlet.*;

import javax.servlet.http.HttpServletRequest;

//import com.dynamide.util.SearchLocations;

public class DynamideFilter implements Filter
{
  private FilterConfig filterConfig;

  public void doFilter (ServletRequest request,
              ServletResponse response,
              FilterChain chain)
  {
    //This should work, but I haven't tested it.  Also, I should grab the user param, and the siteName param.
    RequestDispatcher rd = null;
    try {
        boolean doDynamide = false;
        String siteName = "dynamide"; //%%%%%%%%%% siteName-hack. should allow other sites.
        String userID = "";
        String uri = "";
        if ( request instanceof HttpServletRequest ) {
            uri = ((HttpServletRequest)request).getRequestURI();
        }
        // todo.....  But I don't use this class now.
        if (doDynamide) {
          rd = request.getRequestDispatcher("/dynamide");
          rd.forward(request, response);
        } else {
          chain.doFilter(request, response);
        }
    } catch (IOException io) {
      System.out.println("IOException raised");
    } catch (ServletException se) {
      System.out.println("ServletException raised");
    }
  }

  public void init(FilterConfig config){
    this.filterConfig = config;
  }

  public void destroy(){
    //
  }

  public FilterConfig getFilterConfig(){
    return this.filterConfig;
  }

  public void setFilterConfig (FilterConfig filterConfig){
    this.filterConfig = filterConfig;
  }
}
