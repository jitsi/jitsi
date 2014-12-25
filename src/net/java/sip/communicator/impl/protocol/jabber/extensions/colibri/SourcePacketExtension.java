/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for the <tt>source</tt> element
 * defined by <a href="http://hancke.name/jabber/jingle-sources">
 * Source-Specific Media Attributes in Jingle</a>.
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class SourcePacketExtension
    extends AbstractPacketExtension
{
    /**
     * The XML name of the <tt>setup</tt> element defined by Source-Specific
     * Media Attributes in Jingle.
     */
    public static final String ELEMENT_NAME = "source";

    /**
     * The XML namespace of the <tt>setup</tt> element defined by
     * Source-Specific Media Attributes in Jingle.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:ssma:0";

    /**
     * The XML name of the <tt>setup</tt> element's attribute which corresponds
     * to the <tt>ssrc</tt> media attribute in SDP.
     */
    public static final String SSRC_ATTR_NAME = "ssrc";

    /** Initializes a new <tt>SourcePacketExtension</tt> instance. */
    public SourcePacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Adds a specific parameter (as defined by Source-Specific Media Attributes
     * in Jingle) to this source.
     *
     * @param parameter the <tt>ParameterPacketExtension</tt> to add to this
     * source
     */
    public void addParameter(ParameterPacketExtension parameter)
    {
        addChildExtension(parameter);
    }

    /**
     * Gets the parameters (as defined by Source-Specific Media Attributes in
     * Jingle) of this source.
     *
     * @return the <tt>ParameterPacketExtension</tt>s of this source
     */
    public List<ParameterPacketExtension> getParameters()
    {
        return getChildExtensionsOfType(ParameterPacketExtension.class);
    }

    /**
     * Gets the synchronization source (SSRC) ID of this source.
     *
     * @return the synchronization source (SSRC) ID of this source
     */
    public long getSSRC()
    {
        String s = getAttributeAsString(SSRC_ATTR_NAME);

        return (s == null) ? -1 : Long.parseLong(s);
    }

    /**
     * Sets the synchronization source (SSRC) ID of this source.
     *
     * @param ssrc the synchronization source (SSRC) ID to be set on this source
     */
    public void setSSRC(long ssrc)
    {
        if (ssrc == -1)
            removeAttribute(SSRC_ATTR_NAME);
        else
            setAttribute(SSRC_ATTR_NAME, Long.toString(0xffffffffL & ssrc));
    }

    /**
     * Returns deep copy of this <tt>SourcePacketExtension</tt>.
     */
    public SourcePacketExtension copy()
    {
        SourcePacketExtension copy
            = AbstractPacketExtension.clone(this);

        // COPY SSRC PARAMS
        for (ParameterPacketExtension ppe : getParameters())
        {
            ParameterPacketExtension ppeCopy
                = new ParameterPacketExtension(
                ppe.getName(), ppe.getValue());

            copy.addParameter(ppeCopy);
        }

        return copy;
    }
}
