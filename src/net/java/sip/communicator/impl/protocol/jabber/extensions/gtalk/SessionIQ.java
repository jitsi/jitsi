/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import java.math.*;
import java.security.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jivesoftware.smack.packet.*;

/**
 * A straightforward extension of the IQ. A <tt>SessionIQ</tt> object is created
 * by smack via the {@link SessionIQProvider}. It contains all the information
 * extracted from a <tt>Session</tt> IQ.
 *
 * @author Sebastien Vincent
 */
public class SessionIQ
    extends IQ
{
    /**
     * The name space that session belongs to.
     */
    public static final String NAMESPACE = "http://www.google.com/session";

    /**
     * The name of the element that contains the session data.
     */
    public static final String ELEMENT_NAME = "session";

    /**
     * The name of the argument that contains the session type value.
     */
    public static final String TYPE_ATTR_NAME = "type";

    /**
     * The name of the argument that contains the "initiator" jid.
     */
    public static final String INITIATOR_ATTR_NAME = "initiator";

    /**
     * The name of the argument that contains the session id.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The <tt>GTalkType</tt> that describes the purpose of this
     * <tt>session</tt> element.
     */
    private GTalkType gtalkType;

    /**
     * The full JID of the entity that has initiated the session flow.
     */
    private String initiator = null;

    /**
     * The ID of the GTalk session that this IQ belongs to.
     */
    private String id = null;

    /**
     * The <tt>reason</tt> extension in a <tt>session</tt> IQ providers machine
     * and possibly human-readable information about the reason for the action.
     */
    private ReasonPacketExtension reason = null;

    /**
     * Sets this element's session ID value.
     *
     * @param id the session ID to set
     */
    public void setID(String id)
    {
        this.id = id;
    }

    /**
     * Returns this element's session ID value.
     *
     * @return this element's session ID.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Generates a random <tt>String</tt> usable as a session ID.
     *
     * @return a newly generated random sid <tt>String</tt>
     */
    public static String generateSID()
    {
        return new BigInteger(64, new SecureRandom()).toString(32);
    }

    /**
     * Sets the full JID of the entity that has initiated the session flow. Only
     * present when the <tt>GTalkType</tt> is <tt>accept</tt>.
     *
     * @param initiator the full JID of the initiator.
     */
    public void setInitiator(String initiator)
    {
        this.initiator = initiator;
    }

    /**
     * Returns the full JID of the entity that has initiated the session flow.
     * Only present when the <tt>GTalkType</tt> is <tt>session-accept</tt>.
     *
     * @return the full JID of the initiator.
     */
    public String getInitiator()
    {
        return initiator;
    }

    /**
     * Sets the value of this element's <tt>action</tt> attribute. The value of
     * the 'action' attribute MUST be one of the values enumerated here. If an
     * entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @param gtalkType the value of the <tt>action</tt> attribute.
     */
    public void setGTalkType(GTalkType gtalkType)
    {
        this.gtalkType = gtalkType;
    }

    /**
     * Returns the value of this element's <tt>action</tt> attribute. The value
     * of the 'action' attribute MUST be one of the values enumerated here. If
     * an entity receives a value not defined here, it MUST ignore the attribute
     * and MUST return a <tt>bad-request</tt> error to the sender. There is no
     * default value for the 'action' attribute.
     *
     * @return the value of the <tt>action</tt> attribute.
     */
    public GTalkType getGTalkType()
    {
        return gtalkType;
    }

    /**
     * Specifies this IQ's <tt>reason</tt> extension. The <tt>reason</tt>
     * extension in a <tt>session</tt> IQ provides machine and possibly human
     * -readable information about the reason for the action.
     *
     * @param reason this IQ's <tt>reason</tt> extension.
     */
    public void setReason(ReasonPacketExtension reason)
    {
        this.reason = reason;
    }

    /**
     * Returns this IQ's <tt>reason</tt> extension. The <tt>reason</tt>
     * extension in a <tt>session</tt> IQ provides machine and possibly human
     * -readable information about the reason for the action.
     *
     * @return this IQ's <tt>reason</tt> extension.
     */
    public ReasonPacketExtension getReason()
    {
        return reason;
    }

    /**
     * Returns the XML string of this GTalk session IQ's "section" sub-element.
     *
     * Extensions of this class must override this method.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder bldr = new StringBuilder("<" + ELEMENT_NAME);

        bldr.append(" xmlns='" + NAMESPACE + "'");

        bldr.append(" " + TYPE_ATTR_NAME + "='" + getGTalkType() + "'");

        if( initiator != null)
            bldr.append(" " + INITIATOR_ATTR_NAME
                                + "='" + getInitiator() + "'");

        bldr.append(" " + ID_ATTR_NAME
                            + "='" + getID() + "'");

        String extensionsXML = getExtensionsXML();

        if ((extensionsXML == null) || (extensionsXML.length() == 0) &&
                reason == null)
        {
            bldr.append("/>");
        }
        else
        {
            bldr.append(">");//it is possible to have empty session elements

            //reason
            if (reason != null)
                bldr.append(reason.toXML());

            // extensions
            if ((extensionsXML != null) && (extensionsXML.length() != 0))
                bldr.append(extensionsXML);

            bldr.append("</" + ELEMENT_NAME + ">");
        }

        return bldr.toString();
    }
}
