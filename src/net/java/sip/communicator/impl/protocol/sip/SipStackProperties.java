/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

/**
 * The properties used at the creation of the JAIN-SIP stack.
 *
 * @author Sebastien Mazy
 */
public class SipStackProperties
    extends Properties
{
    private static final long serialVersionUID = 0L;

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of a debug log file.
     */
    private static final String NSPNAME_DEBUG_LOG =
        "gov.nist.javax.sip.DEBUG_LOG";

    /**
     * The default name of a debug log file for the jain-sip RI.
     * (not final on purpose, see contructor)
     */
    private static String NSPVALUE_DEBUG_LOG = "log/sc-jainsipdebug.log";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * the name of the server log file.
     */
    //private static final String NSPNAME_SERVER_LOG
    //    = "gov.nist.javax.sip.SERVER_LOG";

    /**
     * The default name of a server log file for the jain-sip RI.
     * (not final on purpose, see constructor)
     */
    private static String NSPVALUE_SERVER_LOG  = "log/sc-jainsipserver.log";

    /**
     * The name of the property under which the jain-sip-ri would expect to find
     * if the content of the messages (eg SDP) has to be logged
     */
    //private static final String NSPNAME_LOG_MESSAGE_CONTENT
    //    = "gov.nist.javax.sip.LOG_MESSAGE_CONTENT";

    /**
     * A string indicating to jain-sip-ri if the content of the messages (eg
     * SDP) has to be logged
     */
    //private static final String NSPVALUE_LOG_MESSAGE_CONTENT = "true";

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
    private static final String NSPVALUE_DEBUG_LOG_OVERWRITE
        = "true";

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
     * A default specifyier telling the stack whether or not to cache client
     * connections.
     */
    private static final String NSPVALUE_CACHE_CLIENT_CONNECTIONS = "true";

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
    private static final String NSPVALUE_TRACE_LEVEL = "ERROR";

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

        if (!NSPVALUE_SERVER_LOG.startsWith(logDir))
            NSPVALUE_SERVER_LOG = logDir + NSPVALUE_SERVER_LOG;

        this.setProperty(JSPNAME_STACK_NAME, "Sip Communicator");

        // NIST SIP specific properties
        this.setProperty(NSPNAME_DEBUG_LOG, NSPVALUE_DEBUG_LOG);

        // uncomment the following lines to capture messages in the server log
        //this.setProperty(NSPNAME_SERVER_LOG, NSPVALUE_SERVER_LOG);
        //this.setProperty(NSPNAME_LOG_MESSAGE_CONTENT,
        //        NSPVALUE_LOG_MESSAGE_CONTENT);

        this.setProperty(NSPNAME_DEBUG_LOG_OVERWRITE,
                         NSPVALUE_DEBUG_LOG_OVERWRITE);

        this.setProperty(NSPNAME_SERVER_LOG_OVERWRITE,
                         NSPVALUE_SERVER_LOG_OVERWRITE);

        // Drop the client connection after we are done with the transaction.
        this.setProperty(NSPNAME_CACHE_CLIENT_CONNECTIONS,
                NSPVALUE_CACHE_CLIENT_CONNECTIONS);

        // Log level
        this.setProperty(NSPNAME_TRACE_LEVEL, NSPVALUE_TRACE_LEVEL);

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
    }
}
