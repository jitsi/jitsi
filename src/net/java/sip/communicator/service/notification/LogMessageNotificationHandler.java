/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>LogMessageNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * log actions.
 *  
 * @author Yana Stamcheva
 */
public interface LogMessageNotificationHandler
    extends NotificationActionHandler
{
    /**
     * Indicates that this log is of type trace. If this <tt>logType</tt> is set
     * the messages would be logged as trace logs. 
     */
    public static final String TRACE_LOG_TYPE = "TraceLog";
    
    /**
     * Indicates that this log is of type info.  If this <tt>logType</tt> is set
     * the messages would be logged as info logs.
     */
    public static final String INFO_LOG_TYPE = "InfoLog";
    
    /**
     * Indicates that this log is of type error. If this <tt>logType</tt> is set
     * the messages would be logged as error logs. 
     */
    public static final String ERROR_LOG_TYPE = "ErrorLog";
    
    /**
     * Returns the type of the log. One of the XXX_LOG_TYPE-s declared in this
     * interface.
     * @return the type of the log. One of the XXX_LOG_TYPE-s declared in this
     * interface.
     */
    public String getLogType();

    /**
     * Logs the given message.
     * @param message the message to log
     */
    public void logMessage(String message);
}
