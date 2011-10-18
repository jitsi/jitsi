/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>LogMessageNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class LogMessageNotificationHandlerImpl
    implements LogMessageNotificationHandler
{
    /**
     * The logger that will be used to log messages.
     */
    private Logger logger
        = Logger.getLogger(LogMessageNotificationHandlerImpl.class);

    private String logType;

    private boolean isEnabled = true;

    /**
     * Creates an instance of <tt>LogMessageNotificationHandlerImpl</tt> by
     * specifying the log type.
     * 
     * @param logType the type of the log
     */
    public LogMessageNotificationHandlerImpl(String logType)
    {
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

    /**
     * Logs a message through the sip communicator Logger.
     * 
     * @param message the message coming from the event
     */
    public void logMessage(String message)
    {
        if (logType.equals(LogMessageNotificationHandler.ERROR_LOG_TYPE))
            logger.error(message);
        else if(logType.equals(LogMessageNotificationHandler.INFO_LOG_TYPE))
            if (logger.isInfoEnabled())
                logger.info(message);
        else if(logType.equals(LogMessageNotificationHandler.TRACE_LOG_TYPE))
            if (logger.isTraceEnabled())
                logger.trace(message);
    }

    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for the log message action type
     * is disabled no messages will be logged when the
     * <tt>fireNotification</tt> method is called.
     * 
     * @return TRUE if this notification action handler is enabled and FALSE
     * otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Enables or disables this notification handler. While the notification
     * handler for the log message action type is disabled no messages will be
     * logged when the <tt>fireNotification</tt> method is called.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
}
