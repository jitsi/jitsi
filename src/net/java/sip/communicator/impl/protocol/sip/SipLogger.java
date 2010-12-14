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
import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.util.*;

/**
 * This class passes log calls from JAIN-SIP to log4j, so that it is possible
 * to change the log level for the JAIN-SIP stack in logging.properties
 *
 * @author Sebastien Mazy
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
     * logs a stack trace. This helps to look at the stack frame.
     */
    public void logStackTrace()
    {
        if (logger.isTraceEnabled())
            logger.trace("JAIN-SIP stack trace", new Throwable());
    }

    /**
     * logs a stack trace. This helps to look at the stack frame.
     *
     * @param traceLevel currently unused.
     */
    public void logStackTrace(int traceLevel)
    {
        if (logger.isTraceEnabled())
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
     * @param ex the exception that we are to log.
     */
    public void logException(Throwable ex)
    {
        logger.warn("Exception in the JAIN-SIP stack: " + ex.getMessage());
        if (logger.isInfoEnabled())
            logger.info("JAIN-SIP exception stack trace is", ex);

    }

    /**
     * Log a message into the log file.
     *
     * @param message
     *            message to log into the log file.
     */
    public void logDebug(String message)
    {
        if (logger.isDebugEnabled())
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
        if (logger.isTraceEnabled())
            logger.trace("Fatal error from the JAIN-SIP stack: " + message);
    }

    /**
     * Log an error message.
     *
     * @param message error message to log.
     */
    public void logError(String message)
    {
        logger.error("Error from the JAIN-SIP stack: " + message);
    }

    /**
     * Determines whether logging is enabled.
     *
     * @return flag to indicate if logging is enabled.
     */
    public boolean isLoggingEnabled()
    {
        return true;
    }

    /**
     * Return true/false if logging is enabled at a given level.
     *
     * @param logLevel the level that we'd like to check loggability for.
     *
     * @return always <tt>true</tt> regardless of <tt>logLevel</tt>'s value.
     */
    public boolean isLoggingEnabled(int logLevel)
    {
        // always enable trace messages so we can receive packets
        // and log them to packet logging service
        if (logLevel == TRACE_DEBUG)
            return logger.isDebugEnabled();
        if (logLevel == TRACE_MESSAGES)         // same as TRACE_INFO
            return true;
        if (logLevel == TRACE_NONE)
            return false;
        
        return true;
    }

    /**
     * Logs an exception and an error message error message.
     *
     * @param message that message that we'd like to log.
     * @param ex the exception that we'd like to log.
     */
    public void logError(String message, Exception ex)
    {
        logger.error("Error from the JAIN-SIP stack: " + message, ex);
    }

    /**
     * Log a warning message.
     *
     * @param string the warning that we'd like to log
     */
    public void logWarning(String string)
    {
        logger.warn("Warning from the JAIN-SIP stack" + string);
    }

    /**
     * Log an info message.
     *
     * @param string the message that we'd like to log.
     */
    public void logInfo(String string)
    {
        if (logger.isInfoEnabled())
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


    /**
     * Logs the build time stamp of the jain-sip reference implementation.
     *
     * @param buildTimeStamp the build time stamp of the jain-sip reference
     * implementation.
     */
    public void setBuildTimeStamp(String buildTimeStamp)
    {
        if (logger.isTraceEnabled())
            logger.trace("JAIN-SIP RI build " + buildTimeStamp);
    }

    /**
     * Dummy implementation for {@link ServerLogger#setStackProperties(
     * Properties)}
     */
    public void setStackProperties(Properties stackProperties) {}

    /**
     * Dummy implementation for {@link ServerLogger#closeLogFile()}
     */
    public void closeLogFile() {}

    /**
     * Logs the specified message and details.
     *
     * @param message the message to log
     * @param from the message sender
     * @param to the message addressee
     * @param sender determines whether we are the origin of this message.
     * @param time the date this message was received at.
     */
    public void logMessage(SIPMessage message, String from, String to,
                           boolean sender, long time)
    {
        logMessage(message, from, to, null, sender, time);
    }

    /**
     * Logs the specified message and details.
     *
     * @param message the message to log
     * @param from the message sender
     * @param to the message addressee
     * @param status message status
     * @param sender determines whether we are the origin of this message.
     * @param time the date this message was received at.
     */
    public void logMessage(SIPMessage message, String from, String to,
                           String status, boolean sender, long time)
    {
        logPacket(message, sender);
    }

    /**
     * Logs the specified message and details to the packet logging service
     * if enabled.
     *
     * @param message the message to log
     * @param sender determines whether we are the origin of this message.
     */
    public void logPacket(SIPMessage message, boolean sender)
    {
        try
        {
            if(!SipActivator.getPacketLogging().isLoggingEnabled(
                    PacketLoggingService.ProtocolName.SIP))
                return;

            boolean isTransportUDP = message.getTopmostVia().getTransport()
                .equalsIgnoreCase("UDP");

            byte[] srcAddr;
            int srcPort;
            byte[] dstAddr;
            int dstPort;

            if(sender)
            {
                srcAddr = message.getLocalAddress().getAddress();
                srcPort = message.getLocalPort();
                dstAddr = message.getRemoteAddress().getAddress();
                dstPort = message.getRemotePort();
            }
            else
            {
                dstPort = message.getLocalPort();
                dstAddr = message.getLocalAddress().getAddress();
                srcAddr = message.getRemoteAddress().getAddress();
                srcPort = message.getRemotePort();
            }

            byte[] msg = message.toString().getBytes("UTF-8");
            SipActivator.getPacketLogging().logPacket(
                    PacketLoggingService.ProtocolName.SIP,
                    srcAddr, srcPort,
                    dstAddr, dstPort,
                    isTransportUDP ? PacketLoggingService.TransportName.UDP :
                            PacketLoggingService.TransportName.TCP,
                    sender, msg);
        }
        catch(UnsupportedEncodingException e)
        {
            logger.error("Cannot obtain message body", e);
        }
    }

    /**
     * Logs the specified message and details.
     *
     * @param message the message to log
     * @param from the message sender
     * @param to the message addressee
     * @param status message status
     * @param sender determines whether we are the origin of this message.
     */
    public void logMessage(SIPMessage message, String from, String to,
                           String status, boolean sender)
    {
        if (!logger.isInfoEnabled())
            return;

        String msgHeader;

        if(sender)
            msgHeader = "JAIN-SIP sent a message from=\"";
        else
            msgHeader = "JAIN-SIP received a message from=\"";

        if (logger.isInfoEnabled())
            logger.info(msgHeader + from + "\" to=\"" + to + "\" (status: "
                        + status + "):\n" + message);
    }

    /**
     * Prints the specified <tt>exception</tt> as a warning.
     *
     * @param exception the <tt>Exception</tt> we are passed from jain-sip.
     */
    public void logException(Exception exception)
    {
        logger.warn("the following exception occured in JAIN-SIP: "
                        + exception, exception);
    }

    /**
     * A dummy implementation.
     *
     * @param sipStack ignored;
     */
    public void setSipStack(SipStack sipStack) {}

    /**
     * Returns a logger name.
     *
     * @return a logger name.
     */
    public String getLoggerName()
    {
        return "SIP Communicator JAIN SIP logger.";
    }

    /**
     * Logs the specified trace with a debuf level.
     *
     * @param message the trace to log.
     */
    public void logTrace(String message)
    {
        if (logger.isDebugEnabled())
            logger.debug(message);

    }
}
