/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>LogMessageNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class LogMessageNotificationAction
    extends NotificationAction
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

    private String logType;

    /**
     * Creates an instance of <tt>LogMessageNotificationHandlerImpl</tt> by
     * specifying the log type.
     * 
     * @param logType the type of the log
     */
    public LogMessageNotificationAction(String logType)
    {
        super(NotificationAction.ACTION_LOG_MESSAGE);
        this.logType = logType;
    }

    /**
     * Returns the type of the log
     * 
     * @return the type of the log
     */
    public String getLogType()
    {
        return logType;
    }
}
