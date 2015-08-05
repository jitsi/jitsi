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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.net.ssl.*;

import net.java.sip.communicator.impl.protocol.sip.net.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * The properties used at the creation of the JAIN-SIP stack.
 *
 * @author Sebastien Mazy
 * @author Emil Ivov
 */
public class SipStackProperties
    extends Properties
{
    private static final long serialVersionUID = 0L;

    private static final Logger logger
        = Logger.getLogger(SipStackProperties.class);

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a debug log file.
     */
    private static final String NSPNAME_DEBUG_LOG =
        "gov.nist.javax.sip.DEBUG_LOG";

    /**
     * The default name of a debug log file for the jain-sip RI.
     * (not final on purpose, see constructor)
     */
    private static String NSPVALUE_DEBUG_LOG = "log/sc-jainsipdebug.log";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * if the content of the messages (eg SDP) has to be logged
     */
    private static final String NSPNAME_LOG_MESSAGE_CONTENT
        = "gov.nist.javax.sip.LOG_MESSAGE_CONTENT";

    /**
     * A string indicating to jain-sip-ri if the content of the messages (eg
     * SDP) has to be logged
     */
    private static final String NSPVALUE_LOG_MESSAGE_CONTENT = "true";

    /**
     * The name of the property indicating a custom logger class for the stack
     */
    private static final String NSPNAME_STACK_LOGGER
        = "gov.nist.javax.sip.STACK_LOGGER";

    /**
     * The value of the property indicating a custom logger class for the stack
     */
    private static final String NSPVALUE_STACK_LOGGER
        = "net.java.sip.communicator.impl.protocol.sip.SipLogger";

    /**
     * The name of the property indicating a custom logger class
     * for the server messages
     */
    private static final String NSPNAME_SERVER_LOGGER
        = "gov.nist.javax.sip.SERVER_LOGGER";

    /**
     * The value of the property indicating a custom logger class
     * for the server messages
     */
    private static final String NSPVALUE_SERVER_LOGGER
        = "net.java.sip.communicator.impl.protocol.sip.SipLogger";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * if the debug log file has to be overwritten when starting.
     */
    private static final String NSPNAME_DEBUG_LOG_OVERWRITE
        = "gov.nist.javax.sip.DEBUG_LOG_OVERWRITE";

    /**
     * A string indicating to jain-sip-ri if the debug log file has to be
     * overwritten when starting.
     */
    private static final String NSPVALUE_DEBUG_LOG_OVERWRITE = "true";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * if the server log file has to be overwritten when starting.
     */
    private static final String NSPNAME_SERVER_LOG_OVERWRITE
        = "gov.nist.javax.sip.SERVER_LOG_OVERWRITE";

    /**
     * A string indicating to jain-sip-ri if the server log file has to be
     * overwritten when starting.
     */
    private static final String NSPVALUE_SERVER_LOG_OVERWRITE
        = "true";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * a property specifying whether or not it is to cache client connections.
     */
    private static final String NSPNAME_CACHE_CLIENT_CONNECTIONS =
        "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS";

    /**
     * A default specifier telling the stack whether or not to cache client
     * connections.
     */
    private static final String NSPVALUE_CACHE_CLIENT_CONNECTIONS = "true";

    /**
     * The name of the property that tells jain-sip whether that we would like
     * to receive messages from the stack in a concurrent/reentrant/non-blocking
     * manner.
     */
    private static final String NSPNAME_REENTRANT_LISTENER
        = "gov.nist.javax.sip.REENTRANT_LISTENER";

    /**
     * The value of the property that tells jain-sip whether or we would like to
     * receive messages from the stack in a concurrent or blocking manner.
     */
    private static final String NSPVALUE_REENTRANT_LISTENER = "true";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the log level (detail) for all stack logging.
     */
    private static final String NSPNAME_TRACE_LEVEL =
        "gov.nist.javax.sip.TRACE_LEVEL";

    /**
     * A String indicating the default debug level for the jain-sip-ri (must be
     * log4j compatible).
     */
    private static final String NSPVALUE_DEFAULT_TRACE_LEVEL = "ERROR";

    /**
     * The name of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPNAME_DELIVER_UNSOLICITED_NOTIFY =
        "gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY";

    /**
     * The value of the property under which jain-sip will know if it must
     * deliver some unsolicited notify.
     */
    private static final String NSPVALUE_DELIVER_UNSOLICITED_NOTIFY = "true";

    /**
     * The name of the property under which jain-sip will know if it must
     * always use the custom application provided router
     */
    private static final String NSPNAME_USE_ROUTER_FOR_ALL_URIS =
        "javax.sip.USE_ROUTER_FOR_ALL_URIS";

    /**
     * The name of the property under which jain-sip will know if it must
     * always use the custom application provided router
     */
    private static final String NSPVALUE_USE_ROUTER_FOR_ALL_URIS =
        "true";

    /**
     * The name of the property under which jain-sip will know if it must
     * always use the custom application provided router
     */
    private static final String NSPNAME_ROUTER_PATH =
        "javax.sip.ROUTER_PATH";

    /**
     * The name of the property under which jain-sip will know if it must
     * always use the custom application provided router
     */
    private static final String NSPVALUE_ROUTER_PATH =
        "net.java.sip.communicator.impl.protocol.sip.ProxyRouter";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the the name of the stack..
     */
    private static final String JSPNAME_STACK_NAME =
        "javax.sip.STACK_NAME";

    /**
     * The name of the property that defines the supported SSL protocols of the
     * JSIP stack.
     */
    private static final String NSPNAME_TLS_CLIENT_PROTOCOLS =
        "gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS";

    /**
     * Init sip stack properties.
     */
    public SipStackProperties()
    {
        super();

        String logDir
            = SipActivator.getConfigurationService().getScHomeDirLocation()
            + System.getProperty("file.separator")
            + SipActivator.getConfigurationService().getScHomeDirName()
            + System.getProperty("file.separator");

        // don't do it more than one time if many providers are initialised
        if (!NSPVALUE_DEBUG_LOG.startsWith(logDir))
            NSPVALUE_DEBUG_LOG = logDir + NSPVALUE_DEBUG_LOG;

        this.setProperty(JSPNAME_STACK_NAME, "Sip Communicator");

        // NIST SIP specific properties
        this.setProperty(NSPNAME_DEBUG_LOG, NSPVALUE_DEBUG_LOG);

        this.setProperty(NSPNAME_LOG_MESSAGE_CONTENT,
                NSPVALUE_LOG_MESSAGE_CONTENT);

        this.setProperty(NSPNAME_DEBUG_LOG_OVERWRITE,
                         NSPVALUE_DEBUG_LOG_OVERWRITE);

        this.setProperty(NSPNAME_SERVER_LOG_OVERWRITE,
                         NSPVALUE_SERVER_LOG_OVERWRITE);

        // Drop the client connection after we are done with the transaction.
        this.setProperty(NSPNAME_CACHE_CLIENT_CONNECTIONS,
                NSPVALUE_CACHE_CLIENT_CONNECTIONS);

        //handling SIP messages in a reentrant/non-blocking mode
        this.setProperty(NSPNAME_REENTRANT_LISTENER,
                NSPVALUE_REENTRANT_LISTENER);

        // deliver unsolicited NOTIFY
        this.setProperty(NSPNAME_DELIVER_UNSOLICITED_NOTIFY,
                NSPVALUE_DELIVER_UNSOLICITED_NOTIFY);

        // always use custom router for all URIs
        // (the custom router is a wrapper around the default router anyway)
        this.setProperty(NSPNAME_USE_ROUTER_FOR_ALL_URIS,
                NSPVALUE_USE_ROUTER_FOR_ALL_URIS);

        // router to use when no Route header is set
        // our ProxyRouter will send the message to the outbound proxy
        this.setProperty(NSPNAME_ROUTER_PATH,
                NSPVALUE_ROUTER_PATH);

        //make sure that jain-sip would accept java generated addresses
        //containing address scope zones
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");

        //set stack log (trace) level properties according to parameters
        //set in logging.properties for the gov.nist packages
        String logLevel
            = LogManager.getLogManager().getProperty("gov.nist.level");

        String jainSipTraceLevel = null;

        //if this is a java logging level - convert it to a NIST level
        if (logLevel == null)
        {
            jainSipTraceLevel = NSPVALUE_DEFAULT_TRACE_LEVEL;
        }
        else if (logLevel.equals(Level.FINEST.getName()))
        {
            jainSipTraceLevel = "TRACE";
        }
        else if (logLevel.equals(Level.FINER.getName()))
        {
            jainSipTraceLevel = "DEBUG";
        }
        else if (logLevel.equals(Level.FINE.getName()))
        {
            jainSipTraceLevel = "INFO";
        }
        else if (logLevel.equals(Level.WARNING.getName())
                 || logLevel.equals(Level.SEVERE.getName()))
        {
            jainSipTraceLevel = "ERROR";
        }
        else if (logLevel.equals(Level.OFF.getName()))
        {
            jainSipTraceLevel = "OFF";
        }
        else
        {
            //doesn't look like a java logging level, so we just supposed the
            //string was a jain-sip level and pass it directly
            jainSipTraceLevel = logLevel;
        }

        this.setProperty(NSPNAME_TRACE_LEVEL, jainSipTraceLevel);

        this.setProperty(NSPNAME_STACK_LOGGER, NSPVALUE_STACK_LOGGER);
        this.setProperty(NSPNAME_SERVER_LOGGER, NSPVALUE_SERVER_LOGGER);

        if(OSUtils.IS_ANDROID)
        {
            this.setProperty("gov.nist.javax.sip.NETWORK_LAYER",
                AndroidNetworkLayer.class.getName());
        }
        else
        {
            this.setProperty("gov.nist.javax.sip.NETWORK_LAYER",
                SslNetworkLayer.class.getName());
        }

        try
        {
            String enabledSslProtocols = SipActivator.getConfigurationService()
                .getString(NSPNAME_TLS_CLIENT_PROTOCOLS);
            if(StringUtils.isNullOrEmpty(enabledSslProtocols, true))
            {
                SSLSocket temp = (SSLSocket) SSLSocketFactory
                    .getDefault().createSocket();
                String[] enabledDefaultProtocols = temp.getEnabledProtocols();
                enabledSslProtocols = "";
                for(int i = 0; i < enabledDefaultProtocols.length; i++)
                {
                    enabledSslProtocols += enabledDefaultProtocols[i];
                    if(i < enabledDefaultProtocols.length - 1)
                        enabledSslProtocols += ",";
                }
            }
            this.setProperty(NSPNAME_TLS_CLIENT_PROTOCOLS, enabledSslProtocols);
        }
        catch(IOException ex)
        {
            logger.error("Unable to obtain default SSL protocols from Java,"
                + " using JSIP defaults.", ex);
        }
    }
}
