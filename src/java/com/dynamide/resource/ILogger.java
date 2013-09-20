package com.dynamide.resource;

public interface ILogger {
		
	public void connect(ResourceManager rm, String configString) throws Exception;
	
	public void log(org.apache.log4j.Priority priority,
            String sessionid,
            String threadid,
            String stacktrace,
            String message,
            String errorURI);

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
            String errorURI);
	/** Implementations should synchronize this method, since flush might be called from multiple threads.
	 */
	public void flush();
		
	
}
