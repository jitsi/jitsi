/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
            logger.info(message);
        else if(logType.equals(LogMessageNotificationHandler.TRACE_LOG_TYPE))
            logger.trace(message);
    }
}
