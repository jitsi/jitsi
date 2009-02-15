/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallParticipantSecurityFailedEvent</tt> is triggered whenever
 * a problem has occurred during call security process.
 * 
 * @author Yana Stamcheva
 */
public class CallParticipantSecurityMessageEvent
    extends EventObject
{
    /**
     * Indicates that no retained shared secrets are available. The user shall
     * must verify security strings with the other party.
     */
    public static final String SECURITY_AUTHENTICATION_REQUIRED
        = "SecurityAuthenticationRequired";

    /**
     * Indicates that shared secrets retained during previous sessions did not
     * offer valid identifiers. This can happen if the other party uses another
     * client software or lost its stored shared secrets. In rare case this
     * could also signal a Man-In-The-Middle (MITM) attack. Therefore the user
     * shall must verify the SAS with the other party to prove the correct
     * exchange ZRTP data. 
     */
    public static final String RETAINED_SECURITY_AUTHENTICATION_FAILED
        = "RetainedSecurityAuthenticationFailed";

    /**
     * Indicates an internal encryption packet checksum mismatch. In other words
     * the packet was dropped. If this happens often this may indicate a bad
     * connection that corrupts data during transmission. In rare cases and if
     * it happens regularly this could also signal a denial-of-serice attack.
     */
    public static final String CHECKSUM_MISMATCH = "CheckSumMismatch";

    /**
     * Indicates dropping packet because SRTP authentication failed. This may
     * happen if the data was corrupted during transmission or during the very
     * first packets after switching to secure mode. In rare cases and if this
     * happens later during a secure session this could also signal a
     * denial-of-serice attack.
     */
    public static final String AUTHENTICATION_FAILED = "AuthenticationFailed";

    /**
     * Indicates dropping packet because SRTP replay check failed. A duplicate
     * SRTP packet was detected. This may happen if the data was corrupted
     * during transmission. In rare cases and if this happens later during a
     * secure session this could also signal a denial-of-serice attack.
     */
    public static final String REPLAY_CHECK_FAILED = "ReplayCheckFailed";

    /**
     * Indicates too much retries during security negotiation. This may happen
     * if the other party stops to proceed the handshake. Usually if Internet
     * connection is lost or the peer has some problems.
     */
    public static final String RETRY_RATE_EXCEEDED = "RetryRateExceeded";

    /**
     * Indicates that data cannot be send. Internet data connection or peer is
     * down.
     */
    public static final String DATA_SEND_FAILED = "DataSendFailed";
    
    /**
     * Indicates that an internal protocol error occurred. Usually some sort of
     * software problem.
     */
    public static final String INTERNAL_PROTOCOL_ERROR = "InternalProtocolError";

    /**
     * Indicates compatibility problems like for example: unsupported protocol
     * version, unsupported hash type, cypher type, SAS scheme, etc.
     */
    public static final String NOT_COMPATIBLE = "NotCompatible";

    /**
     * Indicates that the other party doesn't support the encryption algorithm
     * we're using or encryption at all.
     */
    public static final String NOT_SUPPORTED = "NotSupported";

    /**
     * Indicates that a general error has occurred.
     */
    public static final String GENERAL_ERROR = "GeneralError";

    /**
     * One of the event types defined in this class.
     */
    private final String eventType;

    /**
     * The message associated with this event.
     */
    private final String eventMessage;

    /**
     * The internationalized message associated with this event.
     */
    private final String eventI18nMessage;

    /**
     * Creates a <tt>CallParticipantSecurityFailedEvent</tt> by specifying the
     * call participant, event type and message associated with this event.
     * 
     * @param callParticipant the call participant implied in this event.
     * @param eventType the type of the event. One of the constants defined in
     * this class.
     * @param eventMessage the message associated with this event.
     * @param i18nMessage the internationalized message associated with this
     * event that could be shown to the user.
     */
    public CallParticipantSecurityMessageEvent(  CallParticipant callParticipant,
                                                String eventType,
                                                String eventMessage,
                                                String i18nMessage)
    {
        super(callParticipant);

        this.eventType = eventType;
        this.eventMessage = eventMessage;
        this.eventI18nMessage = i18nMessage;
    }

    /**
     * Returns the type of this event.
     * 
     * @return the type of this event.
     */
    public String getType()
    {
        return eventType;
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
}
