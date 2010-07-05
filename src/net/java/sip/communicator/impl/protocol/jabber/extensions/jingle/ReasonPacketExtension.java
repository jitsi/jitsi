/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;

/**
 * The <tt>reason</tt> element provides human or machine-readable information
 * explaining what prompted the <tt>action</tt> of the encapsulating
 * <tt>jingle</tt> element.
 *
 * @author Emil Ivov
 */
public class ReasonPacketExtension
    implements PacketExtension
{
    /**
     * The name space (or rather lack thereof ) that the reason element
     * belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the "content" element.
     */
    public static final String ELEMENT_NAME = "reason";

    /**
     * A reason indicating that the party prefers to use an existing session
     * with the peer rather than initiate a new session; the Jingle session ID
     * of the alternative session SHOULD be provided as the XML character data
     * of the <sid/> child.
     */
    public static final String ALTERNATIVE_SESSION = "alternative-session";

    /**
     * A reason indicating that the party is busy and cannot accept a session.
     */
    public static final String BUSY = "busy";

    /**
     * A reason indicating that the initiator wishes to formally cancel the
     * session initiation request.
     */
    public static final String CANCEL = "cancel";

    /**
     * A reason indicating that the action is related to connectivity problems.
     */
    public static final String CONNECTIVITY_ERROR = "connectivity-error";

    /**
     * A reason indicating that the party wishes to formally decline the
     * session.
     */
    public static final String DECLINE = "decline";

    /**
     * A reason indicating that the session length has exceeded a pre-defined
     * time limit (e.g., a meeting hosted at a conference service).
     */
    public static final String EXPIRED = "expired";

    /**
     * A reason indicating that the party has been unable to initialize
     * processing related to the application type.
     */
    public static final String FAILED_APPLICATION = "failed-application";

    /**
     * A reason indicating that the party has been unable to establish
     * connectivity for the transport method.
     */
    public static final String FAILED_TRANSPORT = "failed-transport";

    /**
     * A reason indicating that the action is related to a non-specific
     * application error.
     */
    public static final String GENERAL_ERROR = "general-error";

    /**
     * A reason indicating that the entity is going offline or is no longer
     * available.
     */
    public static final String GONE = "gone";

    /**
     * A reason indicating that the party supports the offered application type
     * but does not support the offered or negotiated parameters.
     */
    public static final String INCOMPATIBLE_PARAMETERS
        = "incompatible-parameters";

    /**
     * A reason indicating that the action is related to media processing
     * problems.
     */
    public static final String MEDIA_ERROR = "media-error";

    /**
     * A reason indicating that the action is related to a violation of local
     * security policies.
     */
    public static final String SECURITY_ERROR = "security-error";

    /**
     * A reason indicating that the action is generated during the normal
     * course of state management and does not reflect any error.
     */
    public static final String SUCCESS = "success";

    /**
     * A reason indicating that a request has not been answered so the sender
     * is timing out the request.
     */
    public static final String TIMEOUT = "timeout";

    /**
     * A reason indicating that the party supports none of the offered
     * application types.
     */
    public static final String UNSUPPORTED_APPLICATIONS
                = "unsupported-applications";

    /**
     * A reason indicating that the party supports none of the offered
     * transport methods.
     */
    public static final String UNSUPPORTED_TRANSPORTS
            = "unsupported-transports";

    /**
     * The reason that this packet extension is transporting.
     */
    private final String reason;

    /**
     * The content of the text element (if any) providing human-readable
     * information about the reason for the action.
     */
    private final String text;

    /**
     * XEP-0166 mentions that the "reason" element MAY contain an element
     * qualified by some other namespace that provides more detailed machine-
     * readable information about the reason for the action.
     */
    private final PacketExtension otherExtension;

    /**
     * Creates a new <tt>ReasonPacketExtension</tt> instance with the specified
     * reason String.
     *
     * @param reason the reason string that we'd like to transport in this
     * packet extension, which may or may not be one of the static strings
     * defined here.
     * @param text an element providing human-readable information about the
     * reason for the action or <tt>null</tt> if no such information is
     * currently available.
     * @param packetExtension any other element that MAY be providing further
     * information or <tt>null</tt> if no such element has been specified.
     */
    public ReasonPacketExtension(String          reason,
                                 String          text,
                                 PacketExtension packetExtension)
    {
        this.reason = reason;
        this.text = text;
        this.otherExtension = packetExtension;
    }

    /**
     * Returns the reason string that this packet extension is transporting.
     *
     * @return the reason string that this packet extension is transporting.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns human-readable information about the reason for the action or
     * <tt>null</tt> if no such information is currently available.
     *
     * @return  human-readable information about the reason for the action or
     * <tt>null</tt> if no such information is currently available.
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns an extra extension containing further info about this action or
     * <tt>null</tt> if no such extension has been specified. This method
     * returns the extension that XEP-0166 refers to the following way:
     * the "reason" element MAY contain an element qualified by some other
     * namespace that provides more detailed machine-readable information about
     * the reason for the action.
     *
     * @return an extra extension containing further info about this action or
     * <tt>null</tt> if no such extension has been specified.
     */
    public PacketExtension getOtherExtension()
    {
        return otherExtension;
    }

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of the PacketExtension.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder("<" + getElementName() + ">");

        bldr.append("<" + getReason() + "/>");

        //add reason "text" if we have it
        if(getText() != null)
        {
            bldr.append("<text>");
            bldr.append(getText());
            bldr.append("</text>");
        }

        //add the extra element if it has been specified.
        if(getOtherExtension() != null)
        {
            bldr.append(getOtherExtension().toXML());
        }

        bldr.append("</" + getElementName() + ">");
        return bldr.toString();
    }
}
