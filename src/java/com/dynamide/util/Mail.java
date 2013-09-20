package com.dynamide.util;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Message;
import javax.mail.Transport;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.dynamide.resource.IContext;
import com.dynamide.resource.ResourceManager;

public class Mail {
    public Mail(){
    }

    public static String lookupHost(){
        Properties props = System.getProperties();
        lookupHostPort(props);
        return props.getProperty("mail.smtp.host", "localhost");
    }

    public static String lookupPort(){
        Properties props = System.getProperties();
        lookupHostPort(props);
        return props.getProperty("mail.smtp.port", "25");
    }

    public static Properties lookupHostPort(Properties props){
        ResourceManager rm = ResourceManager.getRootResourceManager();

        String host = "localhost";
        IContext ctx = (IContext)rm.find("/conf/email-server");
        if ( ctx != null) {
            Object o = ctx.getAttribute("host");
            if (o!=null){
                host = o.toString();
            }
            o = ctx.getAttribute("port");
            if (o!=null){
                String port = o.toString();
                int iport = Tools.stringToIntSafe(port, 0);
                if ( iport > 0 ) {
                    props.put("mail.smtp.port", port);
                }
            }
        }
        host = (host == null || host.length()==0) ? "localhost" : host;
        props.put("mail.smtp.host", host);
        return props;
    }

    public static boolean sendMail(String to, String from, String subject, String body){
        return (sendMail(to, from, subject, body, null).size()==0);
    }


    /** Returns a List of Strings describing any failures, such as incorrect address formats,
     *  or an empty List if no errors; Mail <b><i>is</i></b> sent for all addresses that do not have errors.
     */

    public static List sendMail(String to, String from, String subject, String body, List bcc){
        Vector failures = new Vector();
        try {
            // Get system properties
            Properties props = System.getProperties();

            lookupHostPort(props);  //sets mail.smtp.host as required by javax.mail.Session.
            String host = props.getProperty("mail.smtp.host", "localhost");
            ResourceManager rm = ResourceManager.getRootResourceManager();
            rm.checkMailPermission(host);


            // Get session
            javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);

            // Define message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            if (bcc!= null && bcc.size()>0){
                Iterator it = bcc.iterator();
                while ( it.hasNext() ) {
                    String sAddress = it.next().toString();
                    InternetAddress iad = new InternetAddress(sAddress);
                    try {
                        iad.validate();
                        message.addRecipient(Message.RecipientType.BCC, iad);
                    } catch (Exception e) {
                        failures.add("Address failed: '"+sAddress+"' because: "+e);
                    }
                }
            }

            message.setSubject(subject);

            //this is a hack until I can do real multipart handling:
            if (body.indexOf("<pre>")==-1){
                message.setContent(body, "text/plain");
            } else {
                message.setContent(body, "text/html");
            }

            /*
             * Here's how to do it using both html and plain:
                String plain_text = ... ;
                String html_text  = ... ;
                MimeMultipart content = new MimeMultipart("alternative");
                MimeBodyPart text = new MimeBodyPart();
                MimeBodyPart html = new MimeBodyPart();
                text.setText(plain_text);
                html.setContent(html_text, "text/html");
                content.addBodyPart(text);
                content.addBodyPart(html);
             */

            // Send message
            Transport.send(message);
            Log.debug(Mail.class, "Message sent. \r\nSubject: "+subject+"\r\n<hr />"+body);
        } catch (Exception e){
            Log.error(Mail.class, "ERROR: Message NOT sent!  Transport error: \r\n"+e+"<hr /> \r\nSubject: "+subject+"\r\n<hr />"+body);
            failures.add("ERROR: Message NOT sent!  Transport error: \r\n"+e+"<hr /> \r\nSubject: "+subject+"\r\n<hr />"+body);
        }

        if (failures.size()>0){
            Log.error(Mail.class, "Mail failures:\r\n"+Tools.collectionToString(failures));
        }
        return failures;
    }
}
