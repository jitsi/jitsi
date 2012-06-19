/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The <tt>CallPeerSecurityFailedEvent</tt> is triggered whenever
 * a problem has occurred during call security process.
 *
 * @author Yana Stamcheva
 * @author Werner Dittmann
 */
public class CallPeerSecurityMessageEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * This is a information message. Security will be established.
     */
    public static final int INFORMATION = 0;

    /**
     * This is a warning message. Security will not be established.
     */
    public static final int WARNING = 1;

    /**
     * This is a severe error. Security will not be established.
     */
    public static final int SEVERE = 2;

    /**
     * This is a ZRTP error message. Security will not be established.
     */
    public static final int ERROR = 3;

    /**
     * The internationalized message associated with this event.
     */
    private final String eventI18nMessage;

    /**
     * The message associated with this event.
     */
    private final String eventMessage;

    /**
     * The severity of the security message event.
     */
    private final int eventSeverity;

    /**
     * Creates a <tt>CallPeerSecurityFailedEvent</tt> by specifying the
     * call peer, event type and message associated with this event.
     *
     * @param source the object on which the event initially occurred
     * @param eventMessage the message associated with this event.
     * @param i18nMessage the internationalized message associated with this
     * event that could be shown to the user.
     * @param eventSeverity severity level.
     */
    public CallPeerSecurityMessageEvent(
            Object source,
            String eventMessage,
            String i18nMessage,
            int eventSeverity)
    {
        super(source);

        this.eventMessage = eventMessage;
        this.eventI18nMessage = i18nMessage;
        this.eventSeverity = eventSeverity;
    }

    /**
     * Returns the message associated with this event.
     *
     * @return the message associated with this event.
     */
    public String getMessage()
    {
        return eventMessage;
    }

    /**
     * Returns the internationalized message associated with this event.
     *
     * @return the internationalized message associated with this event.
     */
    public String getI18nMessage()
    {
        return eventI18nMessage;
    }

    /**
     * Returns the event severity.
     *
     * @return the eventSeverity
     */
    public int getEventSeverity() {
        return eventSeverity;
    }
}
