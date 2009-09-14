/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.core.*;
import gov.nist.javax.sip.message.*;
import javax.sip.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * This class passes log calls from JAIN-SIP to log4j, so that it is possible
 * to change the log level for the JAIN-SIP stack in logging.properties
 *
 * @author Sébastien
 */
public class SipLogger
    implements StackLogger,
               ServerLogger
{
    /**
     * All messages will be passed to this logger.
     */
    private static final Logger logger
        = Logger.getLogger(SipLogger.class);

    /*
     * Implementation of StackLogger
     */

    /**
     * log a stack trace. This helps to look at the stack frame.
     */
    public void logStackTrace()
    {
        logger.trace("JAIN-SIP stack trace", new Throwable());
    }

    public void logStackTrace(int traceLevel)
    {
        // FIXE ME: don't ignore the level?
        logger.trace("JAIN-SIP stack trace", new Throwable());
    }

    /**
     * Get the line count in the log stream.
     *
     * @return line count
     */
    public int getLineCount()
    {
        return 0;
    }

    /**
     * Log an exception.
     *
     * @param ex
     */
    public void logException(Throwable ex)
    {
        logger.warn("Exception in the JAIN-SIP stack: " + ex, ex);
    }

    /**
     * Log a message into the log file.
     *
     * @param message
     *            message to log into the log file.
     */
    public void logDebug(String message)
    {
        logger.debug("Debug output from the JAIN-SIP stack: " + message);
    }

    /**
     * Log an error message.
     *
     * @param message --
     *            error message to log.
     */
    public void logFatalError(String message)
    {
        logger.trace("Fatal error from the JAIN-SIP stack: " + message);
    }

    /**
     * Log an error message.
     *
     * @param message --
     *            error message to log.
     *
     */
    public void logError(String message)
    {
        logger.error("Error from the JAIN-SIP stack: " + message);
    }

    /**
     * @return flag to indicate if logging is enabled.
     */
    public boolean isLoggingEnabled()
    {
        return true;
    }

    /**
     * Return true/false if logging is enabled at a given level.
     *
     * @param logLevel
     */
    public boolean isLoggingEnabled(int logLevel)
    {
        return true;
    }

    /**
     * Log an error message.
     *
     * @param message
     * @param ex
     */
    public void logError(String message, Exception ex)
    {
        logger.error("Error from the JAIN-SIP stack: " + message, ex);
    }

    /**
     * Log a warning message.
     *
     * @param string
     */
    public void logWarning(String string)
    {
        logger.warn("Warning from the JAIN-SIP stack" + string);
    }

    /**
     * Log an info message.
     *
     * @param string
     */
    public void logInfo(String string)
    {
        logger.info("Info from the JAIN-SIP stack: " + string);
    }

    /**
     * Disable logging altogether.
     *
     */
    public void disableLogging() {}

    /**
     * Enable logging (globally).
     */
    public void enableLogging() {}

    public void setBuildTimeStamp(String buildTimeStamp)
    {
        logger.trace("JAIN-SIP RI build " + buildTimeStamp);
    }

    public void setStackProperties(Properties stackProperties) {}

    /*
     * Implementation of ServerLogger
     */

    public void closeLogFile() {}

    public void logMessage(SIPMessage message, String from, String to,
                           boolean sender, long time)
    {
        if(sender)
        {
            logger.trace("JAIN-SIP sent message from \"" + from
                         + "\"to \"" + to + "\" at " + time + ":\n"
                         + message);
        }
        else
        {
            logger.trace("JAIN-SIP received message from \"" + from
                         + "\" to \"" + to + "\" at " + time + "\n"
                         + message);
        }
    }

    public void logMessage(SIPMessage message, String from, String to,
                           String status, boolean sender, long time)
    {
        if(sender)
        {
            logger.trace("JAIN-SIP sent message from \"" + from
                         + "\" to \"" + to + "\" at " + time + " (status: "
                         + status + "):\n" + message);
        }
        else
        {
            logger.trace("JAIN-SIP received message from \"" + from
                         + "\" to \"" + to + "\" at " + time + " (status: "
                         + status + "):\n" + message);
        }
    }

    public void logMessage(SIPMessage message, String from, String to,
                           String status, boolean sender)
    {
        if(sender)
        {
            logger.trace("JAIN-SIP sent message from \"" + from
                         + "\" to \"" + to + "\" (status: " + status
                         + "):\n" + message);
        }
        else
        {
            logger.trace("JAIN-SIP received message from \"" + from
                         + "\" to \"" + to + "\" (status: " + status
                         + "):\n" + message); }
    }

    public void logException(Exception ex)
    {
        logger.warn("the following exception occured in JAIN-SIP: " + ex, ex);
    }

    public void setSipStack(SipStack sipStack) {}
}
