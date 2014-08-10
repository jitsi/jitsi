/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.rayo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Rayo 'reason' packet extension.
 *
 * @author Pawel Domas
 */
public class ReasonExtension
    extends AbstractPacketExtension
{
    /**
     * The name of optional platform code attribute.
     */
    public static final String PLATFORM_CODE_ATTRIBUTE = "platform-code";

    /**
     * Indication that the call ended due to a normal hangup by the remote
     * party.
     */
    public static final String HANGUP = "hangup";

    /**
     * Indication that the call ended due to a normal hangup triggered by
     * a hangup command.
     */
    public static final String HANGUP_COMMND = "hangup-command";

    /**
     * Indication that the call ended due to a timeout in contacting the remote
     * party.
     */
    public static final String TIMEOUT = "timeout";

    /**
     * Indication that the call ended due to being rejected by the remote party
     * subsequent to being accepted.
     */
    public static final String BUSY = "busy";

    /**
     * Indication that the call ended due to being rejected by the remote party
     * before being accepted.
     */
    public static final String REJECTED = "rejected";

    /**
     * Indication that the call ended due to a system error.
     */
    public static final String ERROR = "error";

    /**
     * Creates an {@link ReasonExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     *
     * @param elementName the name of the element
     */
    public ReasonExtension(String elementName)
    {
        super(null, elementName);
    }

    /**
     * Returns the value of platform code attribute.
     * @return the value of platform code attribute.
     */
    public String getPlatformCode()
    {
        return getAttributeAsString(PLATFORM_CODE_ATTRIBUTE);
    }

    /**
     * Sets new value of platform code attribute. Pass <tt>null</tt> to remove.
     * @param code new value of platform code attribute. Pass <tt>null</tt> to
     *             remove.
     */
    public void setPlatformCode(String code)
    {
        setAttribute(PLATFORM_CODE_ATTRIBUTE, code);
    }
}
