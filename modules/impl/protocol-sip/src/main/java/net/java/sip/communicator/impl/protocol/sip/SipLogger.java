/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.core.*;
import gov.nist.javax.sip.*;
import gov.nist.javax.sip.message.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.sip.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.packetlogging.*;

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

    /**
     * SipStack to use.
     */
    private SipStack sipStack;

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
        try
        {
            logPacket(message, sender);
        }
        catch(Throwable e)
        {
            logger.error("Error logging packet", e);
        }
    }

    /**
     * Logs the specified message and details to the packet logging service
     * if enabled.
     *
     * @param message the message to log
     * @param sender determines whether we are the origin of this message.
     */
    private void logPacket(SIPMessage message, boolean sender)
    {
        try
        {
            PacketLoggingService packetLogging = SipActivator.getPacketLogging();
            if( packetLogging == null
                || !packetLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.SIP)
                /* Via not present in CRLF packet on TCP - causes NPE */
                || message.getTopmostVia() == null )
                return;

            String transport = message.getTopmostVia().getTransport();
            boolean isTransportUDP = transport.equalsIgnoreCase("UDP");

            byte[] srcAddr;
            int srcPort;
            byte[] dstAddr;
            int dstPort;

            // if addresses are not set use empty byte array with length
            // equals to the other address or just empty
            // byte array with length 4 (ipv4 0.0.0.0)
            if(sender)
            {
                if(!isTransportUDP)
                {
                    InetSocketAddress localAddress =
                        getLocalAddressForDestination(
                            message.getRemoteAddress(),
                            message.getRemotePort(),
                            message.getLocalAddress(),
                            transport);
                    if (localAddress != null)
                    {
                        srcPort = localAddress.getPort();
                        srcAddr = localAddress.getAddress().getAddress();
                    }
                    else
                    {
                        logger.warn("Could not obtain source address for "
                            + " packet. Writing source as 0.0.0.0:0");
                        srcPort = 0;
                        srcAddr = new byte[] { 0, 0, 0, 0 };
                    }
                }
                else
                {
                    srcPort = message.getLocalPort();
                    if(message.getLocalAddress() != null)
                        srcAddr = message.getLocalAddress().getAddress();
                    else if(message.getRemoteAddress() != null)
                        srcAddr = new byte[
                                message.getRemoteAddress().getAddress().length];
                    else
                        srcAddr = new byte[4];
                }

                dstPort = message.getRemotePort();
                if(message.getRemoteAddress() != null)
                    dstAddr = message.getRemoteAddress().getAddress();
                else
                    dstAddr = new byte[srcAddr.length];
            }
            else
            {
                if(!isTransportUDP)
                {
                    InetSocketAddress dstAddress =
                        getLocalAddressForDestination(
                            message.getRemoteAddress(),
                            message.getRemotePort(),
                            message.getLocalAddress(),
                            transport);
                    dstPort = dstAddress.getPort();
                    dstAddr = dstAddress.getAddress().getAddress();
                }
                else
                {
                    dstPort = message.getLocalPort();
                    if(message.getLocalAddress() != null)
                        dstAddr = message.getLocalAddress().getAddress();
                    else if(message.getRemoteAddress() != null)
                        dstAddr = new byte[
                                message.getRemoteAddress().getAddress().length];
                    else
                        dstAddr = new byte[4];
                }

                srcPort = message.getRemotePort();
                if(message.getRemoteAddress() != null)
                    srcAddr = message.getRemoteAddress().getAddress();
                else
                    srcAddr = new byte[dstAddr.length];
            }

            byte[] msg = null;
            if(message instanceof SIPRequest)
            {
                SIPRequest req = (SIPRequest)message;
                if(req.getMethod().equals(SIPRequest.MESSAGE)
                    && message.getContentTypeHeader() != null
                    && message.getContentTypeHeader()
                        .getContentType().equalsIgnoreCase("text"))
                {
                    int len = req.getContentLength().getContentLength();

                    if(len > 0)
                    {
                        SIPRequest newReq = (SIPRequest)req.clone();

                        byte[] newContent =  new byte[len];
                        Arrays.fill(newContent, (byte)'.');
                        newReq.setMessageContent(newContent);
                        msg = newReq.toString().getBytes("UTF-8");
                    }
                }
            }

            if(msg == null)
            {
                msg = message.toString().getBytes("UTF-8");
            }

            packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.SIP,
                    srcAddr, srcPort,
                    dstAddr, dstPort,
                    isTransportUDP ? PacketLoggingService.TransportName.UDP :
                            PacketLoggingService.TransportName.TCP,
                    sender, msg);
        }
        catch(Throwable e)
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
    public void setSipStack(SipStack sipStack)
    {
        this.sipStack = sipStack;
    }

    /**
     * Returns a logger name.
     *
     * @return a logger name.
     */
    public String getLoggerName()
    {
        return "Jitsi JAIN SIP logger.";
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

    /**
     * Returns a local address to use with the specified TCP destination.
     * The method forces the JAIN-SIP stack to create
     * s and binds (if necessary)
     * and return a socket connected to the specified destination address and
     * port and then return its local address.
     *
     * @param dst the destination address that the socket would need to connect
     *            to.
     * @param dstPort the port number that the connection would be established
     * with.
     * @param localAddress the address that we would like to bind on
     * (null for the "any" address).
     * @param transport the transport that will be used TCP ot TLS
     *
     * @return the SocketAddress that this handler would use when connecting to
     * the specified destination address and port.
     *
     * @throws IOException  if we fail binding the local socket
     */
    public java.net.InetSocketAddress getLocalAddressForDestination(
                    java.net.InetAddress dst,
                    int                  dstPort,
                    java.net.InetAddress localAddress,
                    String transport)
        throws IOException
    {
        if(ListeningPoint.TLS.equalsIgnoreCase(transport))
            return (java.net.InetSocketAddress)(((SipStackImpl)this.sipStack)
                .getLocalAddressForTlsDst(dst, dstPort, localAddress));
        else
            return (java.net.InetSocketAddress)(((SipStackImpl)this.sipStack)
            .getLocalAddressForTcpDst(dst, dstPort, localAddress, 0));
    }
}
